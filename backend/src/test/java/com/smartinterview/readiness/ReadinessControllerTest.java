package com.smartinterview.readiness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinterview.entity.*;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class ReadinessControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private JobDescriptionRepository jobDescriptionRepository;
    @Autowired private SkillGapAnalysisRepository skillGapRepository;
    @Autowired private PreparationPlanRepository preparationPlanRepository;
    @Autowired private InterviewSessionRepository interviewSessionRepository;
    @Autowired private InterviewReportRepository interviewReportRepository;

    @Test
    void returnsOwnershipScopedReadinessStrategyAndSupportsRefresh() throws Exception {
        User candidate = candidate();
        SkillGapAnalysis gap = createSkillGap(candidate,
                "[{\"skillName\":\"Java\",\"resumeEvidence\":\"Java\",\"jdRequirement\":\"Java\"}]",
                "[{\"skillName\":\"Spring Boot\",\"resumeEvidence\":\"Spring\",\"jdRequirement\":\"Spring Boot\"}]",
                "[{\"skillName\":\"SQL\",\"resumeEvidence\":null,\"jdRequirement\":\"SQL\"}]"
        );
        String planJson = String.format("{\"skillGapAnalysisId\":%d,\"strongSkills\":[\"Java\"],\"skillAreas\":[{\"skillName\":\"Spring Boot\"},{\"skillName\":\"SQL\"}]}", gap.getId());
        preparationPlanRepository.save(PreparationPlan.builder()
                .user(candidate)
                .title("Spring Boot plan")
                .durationDays(5)
                .planContentJson(planJson)
                .build());

        InterviewSession session = interviewSessionRepository.save(InterviewSession.builder()
                .user(candidate)
                .resume(gap.getResume())
                .jobDescription(gap.getJobDescription())
                .sessionTitle("Java backend interview")
                .targetRole("Java Backend Developer")
                .interviewType(InterviewType.TECHNICAL)
                .status(InterviewStatus.COMPLETED)
                .targetQuestionCount(3)
                .completedQuestionCount(3)
                .startedAt(LocalDateTime.now().minusMinutes(30))
                .completedAt(LocalDateTime.now())
                .build());
        interviewReportRepository.save(InterviewReport.builder()
                .interviewSession(session)
                .overallScore(62.0)
                .technicalScore(64.0)
                .communicationScore(70.0)
                .problemSolvingScore(58.0)
                .executiveSummary("Candidate is improving")
                .strengthsJson("[\"Java\"]")
                .weaknessesJson("[\"Spring Boot\"]")
                .claimVerificationsJson("[]")
                .preparationRoadmapJson("[]")
                .generatedAt(LocalDateTime.now())
                .build());

        String token = login("candidate@smartinterview.ai", "Candidate@123456");

        MvcResult result = mockMvc.perform(get("/api/v1/readiness")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallReadiness").isNumber())
                .andExpect(jsonPath("$.strongSkills[0].skillName").value("Java"))
                .andExpect(jsonPath("$.improvementAreas[0].skillName").value("Spring Boot"))
                .andExpect(jsonPath("$.dailyGoals.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.nextRecommendedAction").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(body.get("overallReadiness").asInt() > 0);

        mockMvc.perform(post("/api/v1/readiness/refresh")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallReadiness").isNumber());

        mockMvc.perform(get("/api/v1/readiness")
                        .header("Authorization", "Bearer " + login("admin@smartinterview.ai", "Admin@123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallReadiness").value(0));
    }

    @Test
    void returnsEmptyStateWithoutUserData() throws Exception {
        String token = login("candidate@smartinterview.ai", "Candidate@123456");

        mockMvc.perform(get("/api/v1/readiness")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallReadiness").value(0))
                .andExpect(jsonPath("$.strongSkills.length()").value(0))
                .andExpect(jsonPath("$.improvementAreas.length()").value(0))
                .andExpect(jsonPath("$.dailyGoals.length()").value(0));
    }

    private User candidate() {
        return userRepository.findByEmail("candidate@smartinterview.ai").orElseThrow();
    }

    private SkillGapAnalysis createSkillGap(User user, String matched, String partial, String missing) {
        Resume resume = resumeRepository.save(Resume.builder()
                .user(user)
                .fileName("resume.pdf")
                .originalFileName("resume.pdf")
                .filePath("resumes/resume.pdf")
                .fileSize(100L)
                .contentType("application/pdf")
                .rawText("Java backend skills")
                .build());
        JobDescription jd = jobDescriptionRepository.save(JobDescription.builder()
                .user(user)
                .title("Java Backend Developer")
                .rawText("Java backend role with Spring Boot and SQL")
                .build());
        return skillGapRepository.save(SkillGapAnalysis.builder()
                .resume(resume)
                .jobDescription(jd)
                .matchPercentage(55.0)
                .matchedSkillsJson(matched)
                .partialSkillsJson(partial)
                .missingSkillsJson(missing)
                .recommendationsJson("[]")
                .analyzedAt(LocalDateTime.now())
                .build());
    }

    private String login(String email, String password) throws Exception {
        String payload = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
