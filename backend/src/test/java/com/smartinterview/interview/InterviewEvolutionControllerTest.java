package com.smartinterview.interview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinterview.entity.*;
import com.smartinterview.entity.enums.DifficultyLevel;
import com.smartinterview.entity.enums.InterviewStatus;
import com.smartinterview.entity.enums.InterviewType;
import com.smartinterview.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class InterviewEvolutionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private InterviewSessionRepository sessionRepository;
    @Autowired private InterviewQuestionRepository questionRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private ResumeAnalysisRepository resumeAnalysisRepository;
    @Autowired private JobDescriptionRepository jdRepository;
    @Autowired private JobDescriptionAnalysisRepository jdAnalysisRepository;
    @Autowired private SkillGapAnalysisRepository skillGapRepository;
    @Autowired private PreparationPlanRepository planRepository;
        @Autowired private InterviewEvolutionService evolutionService;

    @Test
    void profilesOnlyTheAuthenticatedUsersPersistedInterviewPerformance() throws Exception {
        User candidate = candidate();
        createCompletedInterview(candidate, "Backend baseline", List.of(
                new Result("Java", 9.0), new Result("SQL", 2.0), new Result("SQL", 3.0)));
        String candidateToken = login("candidate@smartinterview.ai", "Candidate@123456");
        String adminToken = login("admin@smartinterview.ai", "Admin@123456");

        mockMvc.perform(get("/api/v1/interview/adaptive/profile")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasHistory").value(true))
                .andExpect(jsonPath("$.completedInterviewCount").value(1))
                .andExpect(jsonPath("$.skillProfile[0].skillName").value("SQL"))
                .andExpect(jsonPath("$.skillProfile[0].scorePercent").value(25.0))
                .andExpect(jsonPath("$.skillProfile[0].level").value("WEAK"))
                .andExpect(jsonPath("$.skillProfile[0].previousMistakes").value(2))
                .andExpect(jsonPath("$.skillProfile[1].level").value("STRONG"))
                .andExpect(jsonPath("$.history[0].overallScorePercent").value(46.7));

        mockMvc.perform(get("/api/v1/interview/adaptive/profile")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasHistory").value(false))
                .andExpect(jsonPath("$.skillProfile.length()").value(0));
        mockMvc.perform(get("/api/v1/interview/adaptive/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adaptiveStartTargetsWeakRoleGapAndPersistsEvidenceBasedReasons() throws Exception {
        User candidate = candidate();
        JobDescription jd = createJobDescription(candidate, List.of("Java", "SQL"));
        Resume resume = createResume(candidate, List.of("Java"));
        createCompletedInterview(candidate, "Backend baseline", List.of(
                new Result("Java", 9.0), new Result("SQL", 2.0), new Result("SQL", 3.0)));
        SkillGapAnalysis gap = skillGapRepository.save(SkillGapAnalysis.builder()
                .resume(resume).jobDescription(jd).matchPercentage(50.0)
                .matchedSkillsJson("[]").partialSkillsJson("[]")
                .missingSkillsJson("[{\"skillName\":\"SQL\",\"jdRequirement\":\"SQL\"}]")
                .recommendationsJson("[]").analyzedAt(LocalDateTime.now()).build());
        planRepository.save(PreparationPlan.builder().user(candidate).title("SQL interview practice")
                .durationDays(4).planContentJson("{\"skillGapAnalysisId\":" + gap.getId()
                        + ",\"skillAreas\":[{\"skillName\":\"SQL\"}]}").build());
        String token = login("candidate@smartinterview.ai", "Candidate@123456");

        MvcResult started = mockMvc.perform(post("/api/v1/interview/adaptive/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(resume.getId(), jd.getId(), 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adaptiveMode").value(true))
                .andExpect(jsonPath("$.currentQuestion.targetSkill").value("SQL"))
                .andExpect(jsonPath("$.currentQuestion.difficultyLevel").value("EASY"))
                .andReturn();
        JsonNode body = objectMapper.readTree(started.getResponse().getContentAsString());
        Set<String> reasons = reasonCategories(body.at("/currentQuestion/selectionReasons"));
        assertTrue(reasons.containsAll(Set.of("SKILL_GAP", "PREVIOUS_MISTAKE", "LOW_PERFORMANCE",
                "TARGET_ROLE_REQUIREMENT", "PREPARATION_PLAN", "DIFFICULTY_PROGRESSION")), reasons.toString());

        long sessionId = body.get("sessionId").asLong();
        long questionId = body.at("/currentQuestion/questionId").asLong();
        List<String> selectedSkills = new ArrayList<>();
        selectedSkills.add(body.at("/currentQuestion/targetSkill").asText());
        for (int answered = 1; answered < 5; answered++) {
            MvcResult next = mockMvc.perform(post("/api/v1/interview/{id}/answer/{questionId}", sessionId, questionId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"answerText\":\"SQL joins combine related rows using a matching key. I would use an inner join when both sides must match, and a left join when all records from the first table must remain. I would verify the result with representative rows and indexes.\",\"timeTakenSeconds\":40}"))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode updated = objectMapper.readTree(next.getResponse().getContentAsString());
            questionId = updated.at("/currentQuestion/questionId").asLong();
            if (questionId > 0) selectedSkills.add(updated.at("/currentQuestion/targetSkill").asText());
        }
        assertTrue(selectedSkills.stream().filter("SQL"::equals).count()
                > selectedSkills.stream().filter("Java"::equals).count(), selectedSkills.toString());

        String adminToken = login("admin@smartinterview.ai", "Admin@123456");
        mockMvc.perform(get("/api/v1/interview/{id}", sessionId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void givesOnlyTargetRequirementReasonWhenNoOtherEvidenceExists() throws Exception {
        User candidate = candidate();
        JobDescription jd = createJobDescription(candidate, List.of("Java", "SQL"));
        String token = login("candidate@smartinterview.ai", "Candidate@123456");

        MvcResult result = mockMvc.perform(post("/api/v1/interview/adaptive/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(null, jd.getId(), 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentQuestion.targetSkill").value("Java"))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Set<String> reasons = reasonCategories(body.at("/currentQuestion/selectionReasons"));
        assertEquals(Set.of("TARGET_ROLE_REQUIREMENT"), reasons);
        assertEquals("HARD", body.at("/currentQuestion/difficultyLevel").asText());
    }

    @Test
    void explainsSessionDifficultyChangeWhenFocusMovesToAnotherSkill() throws Exception {
        User candidate = candidate();
        JobDescription jd = createJobDescription(candidate, List.of("REST APIs", "SQL"));
        Resume resume = createResume(candidate, List.of("Java"));
        SkillGapAnalysis gap = skillGapRepository.save(SkillGapAnalysis.builder().resume(resume).jobDescription(jd)
                .matchPercentage(0.0).matchedSkillsJson("[]").partialSkillsJson("[]")
                .missingSkillsJson("[{\"skillName\":\"SQL\",\"jdRequirement\":\"SQL\"}]")
                .recommendationsJson("[]").analyzedAt(LocalDateTime.now()).build());
        planRepository.save(PreparationPlan.builder().user(candidate).title("SQL plan")
                .durationDays(4).planContentJson("{\"skillGapAnalysisId\":" + gap.getId()
                        + ",\"skillAreas\":[{\"skillName\":\"SQL\"}]}").build());
        InterviewSession session = sessionRepository.save(InterviewSession.builder().user(candidate)
                .jobDescription(jd).resume(resume).sessionTitle("In-progress adaptive")
                .targetRole("Backend Engineer").interviewType(InterviewType.TECHNICAL)
                .status(InterviewStatus.IN_PROGRESS).adaptiveMode(true)
                .currentDifficulty(DifficultyLevel.EASY).targetQuestionCount(3).build());
        createAnsweredQuestion(session, 1, "REST APIs", 3.7);

        AdaptiveQuestionPlan next = evolutionService.selectQuestion(session, 2);

        assertEquals("SQL", next.focusSkill());
        assertEquals(DifficultyLevel.EASY, next.difficulty());
        assertTrue(next.reasons().stream().anyMatch(reason -> reason.category().equals("DIFFICULTY_PROGRESSION")
                && reason.description().contains("REST APIs answer scored 3.7/10")));
    }

    private User candidate() {
        return userRepository.findByEmail("candidate@smartinterview.ai").orElseThrow();
    }

    private JobDescription createJobDescription(User user, List<String> requiredSkills) {
        JobDescription jd = jdRepository.save(JobDescription.builder().user(user).title("Backend Engineer")
                .rawText("Java and SQL backend role").build());
        JobDescriptionAnalysis analysis = jdAnalysisRepository.save(JobDescriptionAnalysis.builder().jobDescription(jd)
                .roleTitle("Backend Engineer").requiredSkillsJson(toJson(requiredSkills))
                .niceToHaveSkillsJson("[]").minimumExperienceYears(2.0)
                .roleSummary("Backend role").rawAiResponse("{}").build());
        jd.setAnalysis(analysis);
        return jd;
    }

    private Resume createResume(User user, List<String> skills) {
        Resume resume = resumeRepository.save(Resume.builder().user(user).fileName("resume.pdf")
                .originalFileName("resume.pdf").filePath("resumes/resume.pdf").fileSize(100L)
                .contentType("application/pdf").rawText("Java backend experience").build());
        resumeAnalysisRepository.save(ResumeAnalysis.builder().resume(resume)
                .extractedSkillsJson(toJson(skills)).experienceYears(4.0).summary("Backend engineer")
                .domainsJson("[]").educationJson("[]").rawAiResponse("{}").build());
        return resume;
    }

    private InterviewSession createCompletedInterview(User user, String title, List<Result> results) {
        LocalDateTime startedAt = LocalDateTime.now().minusDays(1);
        InterviewSession session = sessionRepository.save(InterviewSession.builder().user(user).sessionTitle(title)
                .targetRole("Backend Engineer").interviewType(InterviewType.TECHNICAL)
                .status(InterviewStatus.COMPLETED).currentDifficulty(DifficultyLevel.MEDIUM)
                .targetQuestionCount(results.size()).completedQuestionCount(results.size())
                .startedAt(startedAt).completedAt(startedAt.plusMinutes(20)).build());
        int order = 0;
        for (Result result : results) {
            order++;
            InterviewQuestion question = InterviewQuestion.builder().interviewSession(session)
                    .questionOrder(order).questionText("Explain " + result.skill() + " for a backend service.")
                    .category("Technical").targetSkill(result.skill()).difficultyLevel(DifficultyLevel.MEDIUM)
                    .expectedKeyPointsJson("[]").createdAt(startedAt.plusMinutes(order)).build();
            CandidateAnswer answer = CandidateAnswer.builder().interviewQuestion(question)
                    .answerText("Previous " + result.skill() + " answer").submittedAt(startedAt.plusMinutes(order)).build();
            AnswerEvaluation evaluation = AnswerEvaluation.builder().candidateAnswer(answer)
                    .technicalAccuracyScore(result.score()).completenessScore(result.score())
                    .clarityScore(result.score()).overallScore(result.score())
                    .strengths("Explained a concept").areasOfImprovement(result.score() < 5 ? "Add details" : "")
                    .detailedFeedback("{\"communicationScore\":" + result.score() + "}")
                    .evaluatedAt(startedAt.plusMinutes(order)).build();
            answer.setEvaluation(evaluation);
            question.setCandidateAnswer(answer);
            questionRepository.save(question);
        }
        return session;
    }

    private void createAnsweredQuestion(InterviewSession session, int order, String skill, double score) {
        InterviewQuestion question = InterviewQuestion.builder().interviewSession(session).questionOrder(order)
                .questionText("Explain " + skill).category("Technical").targetSkill(skill)
                .difficultyLevel(DifficultyLevel.MEDIUM).expectedKeyPointsJson("[]")
                .createdAt(LocalDateTime.now()).build();
        CandidateAnswer answer = CandidateAnswer.builder().interviewQuestion(question)
                .answerText("I do not know this topic yet.").submittedAt(LocalDateTime.now()).build();
        answer.setEvaluation(AnswerEvaluation.builder().candidateAnswer(answer).overallScore(score)
                .technicalAccuracyScore(score).completenessScore(score).clarityScore(score)
                .strengths("Starting point").areasOfImprovement("Explain details")
                .detailedFeedback("{}").evaluatedAt(LocalDateTime.now()).build());
        question.setCandidateAnswer(answer);
        questionRepository.save(question);
    }

    private String request(Long resumeId, Long jdId, int questions) throws Exception {
        var request = objectMapper.createObjectNode().put("sessionTitle", "Adaptive practice")
                .put("targetRole", "Backend Engineer").put("interviewType", "TECHNICAL")
                .put("targetQuestionCount", questions).put("difficulty", "HARD");
        if (resumeId != null) request.put("resumeId", resumeId);
        if (jdId != null) request.put("jobDescriptionId", jdId);
        return objectMapper.writeValueAsString(request);
    }

    private Set<String> reasonCategories(JsonNode reasons) {
        Set<String> categories = new java.util.HashSet<>();
        reasons.forEach(reason -> categories.add(reason.path("category").asText()));
        return categories;
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    private record Result(String skill, double score) {}
}