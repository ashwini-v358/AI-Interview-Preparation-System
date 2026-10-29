package com.smartinterview.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinterview.entity.InterviewSession;
import com.smartinterview.entity.JobDescription;
import com.smartinterview.entity.Resume;
import com.smartinterview.entity.SkillGapAnalysis;
import com.smartinterview.entity.User;
import com.smartinterview.entity.enums.InterviewStatus;
import com.smartinterview.entity.enums.InterviewType;
import com.smartinterview.interview.InterviewContextSignals;
import com.smartinterview.interview.InterviewEvolutionService;
import com.smartinterview.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the real-Gemini wiring end to end while Gemini is deliberately unavailable.
 *
 * Configuration: {@code app.ai.provider=gemini} with an empty API key and an unroutable base URL,
 * so every provider call fails immediately and deterministically without any network access.
 * The assertions then verify that the candidate still gets a working interview instead of a 5xx,
 * that authentication and ownership are unchanged, and that AI context stays scoped to its owner.
 */
@SpringBootTest(properties = {
        "app.ai.provider=gemini",
        "app.ai.api-key=",
        "app.ai.gemini.base-url=http://127.0.0.1:1"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class GeminiProviderInterviewFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ApplicationContext applicationContext;
    @Autowired private FallbackAIService fallbackAIService;
    @Autowired private InterviewEvolutionService evolutionService;
    @Autowired private UserRepository userRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private JobDescriptionRepository jobDescriptionRepository;
    @Autowired private SkillGapAnalysisRepository skillGapRepository;
    @Autowired private InterviewSessionRepository sessionRepository;

    @Test
    void injectsTheGeminiProviderWithMockFallbackAsTheActiveAiService() {
        AIService active = applicationContext.getBean(AIService.class);

        assertInstanceOf(FallbackAIService.class, active);
        assertNotNull(applicationContext.getBean(GeminiAIService.class),
                "GeminiAIService must be registered when app.ai.provider=gemini");
        assertNotNull(applicationContext.getBean(MockAIService.class),
                "MockAIService must stay available as the deterministic fallback");
    }

    @Test
    void startsAnInterviewAndKeepsGeneratingQuestionsWhileGeminiIsUnavailable() throws Exception {
        String token = login("candidate@smartinterview.ai", "Candidate@123456");
        long fallbacksBefore = fallbackAIService.fallbackCount();

        MvcResult started = mockMvc.perform(post("/api/v1/interview/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionTitle\":\"Gemini outage interview\",\"targetRole\":\"Java Backend Developer\","
                                + "\"interviewType\":\"TECHNICAL\",\"targetQuestionCount\":3,\"difficulty\":\"MEDIUM\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentQuestion.questionId", notNullValue()))
                .andExpect(jsonPath("$.currentQuestion.questionText", notNullValue()))
                .andExpect(jsonPath("$.currentQuestion.difficultyLevel").value("MEDIUM"))
                .andReturn();

        JsonNode body = objectMapper.readTree(started.getResponse().getContentAsString());
        long sessionId = body.get("sessionId").asLong();
        long questionId = body.at("/currentQuestion/questionId").asLong();
        assertFalse(body.at("/currentQuestion/questionText").asText().isBlank());

        MvcResult answered = mockMvc.perform(post("/api/v1/interview/{id}/answer/{questionId}", sessionId, questionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerText\":\"I used Spring Boot with MySQL and indexed the frequently "
                                + "queried columns so the reporting endpoint stayed fast.\",\"timeTakenSeconds\":45}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedQuestionCount").value(1))
                .andExpect(jsonPath("$.currentQuestion.questionId", notNullValue()))
                .andExpect(jsonPath("$.currentQuestion.questionText", notNullValue()))
                .andReturn();

        JsonNode response = objectMapper.readTree(answered.getResponse().getContentAsString());
        assertFalse(response.at("/currentQuestion/questionText").asText().isBlank());
        assertTrue(response.at("/history/0/overallScore").isNumber());
        assertTrue(fallbackAIService.fallbackCount() > fallbacksBefore,
                "Every generation call should have been served by the mock fallback");
    }

    @Test
    void keepsAuthenticationAndOwnershipChecksIntact() throws Exception {
        String candidateToken = login("candidate@smartinterview.ai", "Candidate@123456");
        String adminToken = login("admin@smartinterview.ai", "Admin@123456");

        mockMvc.perform(get("/api/v1/interview/my").header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/interview/my"))
                .andExpect(status().isUnauthorized());

        MvcResult started = mockMvc.perform(post("/api/v1/interview/start")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionTitle\":\"Ownership check\",\"targetRole\":\"Java Backend Developer\","
                                + "\"targetQuestionCount\":3}"))
                .andExpect(status().isCreated())
                .andReturn();
        long sessionId = objectMapper.readTree(started.getResponse().getContentAsString()).get("sessionId").asLong();

        mockMvc.perform(get("/api/v1/interview/{id}", sessionId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void scopesQuestionContextSignalsToTheOwningCandidateOnly() {
        User candidate = user("candidate@smartinterview.ai");
        User admin = user("admin@smartinterview.ai");
        CandidateSource candidateSource = createCandidateSource(candidate, "SQL");
        CandidateSource adminSource = createCandidateSource(admin, "Kubernetes");

        InterviewContextSignals candidateSignals = evolutionService.loadContextSignals(
                createSession(candidate, candidateSource));
        InterviewContextSignals adminSignals = evolutionService.loadContextSignals(
                createSession(admin, adminSource));

        assertTrue(candidateSignals.skillGapSignals().stream()
                        .anyMatch(signal -> "SQL".equals(signal.skill()) && "MISSING".equals(signal.level())),
                candidateSignals.skillGapSignals().toString());
        assertTrue(candidateSignals.skillGapSignals().stream()
                        .noneMatch(signal -> "Kubernetes".equals(signal.skill())),
                "another candidate's skill gap must never reach this candidate's AI prompt");
        assertTrue(adminSignals.skillGapSignals().stream()
                        .noneMatch(signal -> "SQL".equals(signal.skill())),
                adminSignals.skillGapSignals().toString());
    }

    @Test
    void doesNotLeakTheConfiguredApiKeyThroughAnyResponse() throws Exception {
        String token = login("candidate@smartinterview.ai", "Candidate@123456");

        MvcResult started = mockMvc.perform(post("/api/v1/interview/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionTitle\":\"Secret check\",\"targetRole\":\"Java Backend Developer\","
                                + "\"targetQuestionCount\":3}"))
                .andExpect(status().isCreated())
                .andReturn();

        String body = started.getResponse().getContentAsString();
        assertFalse(body.contains("x-goog-api-key"), body);
        assertFalse(body.toLowerCase().contains("apikey"), body);
    }

    // ─── Fixtures ───────────────────────────────────────────────────────────

    private InterviewSession createSession(User user, CandidateSource source) {
        return sessionRepository.save(InterviewSession.builder()
                .user(user)
                .resume(source.resume())
                .jobDescription(source.jobDescription())
                .sessionTitle("Signal scope session")
                .targetRole("Java Backend Developer")
                .interviewType(InterviewType.TECHNICAL)
                .status(InterviewStatus.IN_PROGRESS)
                .targetQuestionCount(3)
                .startedAt(LocalDateTime.now())
                .build());
    }

    private CandidateSource createCandidateSource(User user, String missingSkill) {
        Resume resume = resumeRepository.save(Resume.builder().user(user)
                .fileName("resume.pdf").originalFileName("resume.pdf")
                .filePath("resumes/resume.pdf").fileSize(100L)
                .contentType("application/pdf").rawText("Java backend experience").build());
        JobDescription jd = jobDescriptionRepository.save(JobDescription.builder().user(user)
                .title("Java Backend Developer")
                .rawText("Java, Spring Boot, " + missingSkill).build());
        skillGapRepository.save(SkillGapAnalysis.builder().resume(resume).jobDescription(jd)
                .matchPercentage(50.0).matchedSkillsJson("[]").partialSkillsJson("[]")
                .missingSkillsJson("[{\"skillName\":\"" + missingSkill + "\",\"jdRequirement\":\""
                        + missingSkill + "\"}]")
                .recommendationsJson("[]").analyzedAt(LocalDateTime.now()).build());
        return new CandidateSource(resume, jd);
    }

    private User user(String email) {
        return userRepository.findByEmail(email).orElseThrow();
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private record CandidateSource(Resume resume, JobDescription jobDescription) {}
}
