package com.smartinterview.preparation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinterview.entity.JobDescription;
import com.smartinterview.entity.PreparationPlan;
import com.smartinterview.entity.Resume;
import com.smartinterview.entity.SkillGapAnalysis;
import com.smartinterview.entity.User;
import com.smartinterview.repository.JobDescriptionRepository;
import com.smartinterview.repository.PreparationPlanRepository;
import com.smartinterview.repository.ResumeRepository;
import com.smartinterview.repository.SkillGapAnalysisRepository;
import com.smartinterview.repository.UserRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class PreparationPlanControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private JobDescriptionRepository jobDescriptionRepository;
    @Autowired private SkillGapAnalysisRepository skillGapRepository;
    @Autowired private PreparationPlanRepository planRepository;

    @Test
    void generatesPersistsAndRetrievesOwnerPlanFromSkillGaps() throws Exception {
        User user = candidate();
        SkillGapAnalysis gap = createGap(user,
                "[{\"skillName\":\"Java\",\"resumeEvidence\":\"Java\",\"jdRequirement\":\"Java\"}]",
                "[{\"skillName\":\"Spring Boot\",\"resumeEvidence\":\"Spring\",\"jdRequirement\":\"Spring Boot\"}]",
                "[{\"skillName\":\"SQL\",\"resumeEvidence\":null,\"jdRequirement\":\"SQL\"}]");
        String token = login("candidate@smartinterview.ai", "Candidate@123456");

        MvcResult generated = mockMvc.perform(post("/api/v1/preparation-plan/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Preparation Plan: Backend Engineer"))
                .andExpect(jsonPath("$.durationDays").value(6))
                .andExpect(jsonPath("$.skillGapAnalysisId").value(gap.getId()))
                .andExpect(jsonPath("$.strongSkills[0]").value("Java"))
                .andExpect(jsonPath("$.skillAreas[0].skillName").value("Spring Boot"))
                .andExpect(jsonPath("$.skillAreas[0].currentStatus").value("WEAK"))
                .andExpect(jsonPath("$.skillAreas[0].priority").value("MEDIUM"))
                .andExpect(jsonPath("$.skillAreas[0].recommendedTopics.length()").value(3))
                .andExpect(jsonPath("$.skillAreas[0].practiceTasks.length()").value(2))
                .andExpect(jsonPath("$.skillAreas[0].interviewSuggestions.length()").value(2))
                .andExpect(jsonPath("$.skillAreas[1].currentStatus").value("MISSING"))
                .andExpect(jsonPath("$.skillAreas[1].priority").value("HIGH"))
                .andReturn();
        JsonNode first = objectMapper.readTree(generated.getResponse().getContentAsString());
        assertEquals(1, planRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).size());

        mockMvc.perform(get("/api/v1/preparation-plan")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(first.get("planId").asLong()));
    }

    @Test
    void regeneratesAsANewPlanVersionAndReturnsTheLatest() throws Exception {
        User user = candidate();
        createGap(user, "[]", "[]",
                "[{\"skillName\":\"Kubernetes\",\"resumeEvidence\":null,\"jdRequirement\":\"Kubernetes\"}]");
        String token = login("candidate@smartinterview.ai", "Candidate@123456");

        JsonNode first = generate(token);
        JsonNode second = generate(token);

        assertEquals(2, planRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).size());
        mockMvc.perform(get("/api/v1/preparation-plan")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(second.get("planId").asLong()))
                .andExpect(jsonPath("$.planId").value(org.hamcrest.Matchers.not(first.get("planId").asLong())));
    }

    @Test
    void servesOnlyOwnerPlanAndReturnsEmptyGapPlanWhenAllSkillsMatch() throws Exception {
        User user = candidate();
        createGap(user,
                "[{\"skillName\":\"Java\",\"resumeEvidence\":\"Java\",\"jdRequirement\":\"Java\"}]",
                "[]", "[]");
        String token = login("candidate@smartinterview.ai", "Candidate@123456");
        JsonNode generated = generate(token);

        mockMvc.perform(get("/api/v1/preparation-plan")
                        .header("Authorization", "Bearer " + login("admin@smartinterview.ai", "Admin@123456")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/preparation-plan")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(generated.get("planId").asLong()))
                .andExpect(jsonPath("$.durationDays").value(0))
                .andExpect(jsonPath("$.skillAreas.length()").value(0))
                .andExpect(jsonPath("$.strongSkills[0]").value("Java"));
    }

    @Test
    void requiresSkillGapBeforeGenerationAndReturnsNotFoundForEmptyPlan() throws Exception {
        String token = login("candidate@smartinterview.ai", "Candidate@123456");
        mockMvc.perform(post("/api/v1/preparation-plan/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/preparation-plan")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private User candidate() {
        return userRepository.findByEmail("candidate@smartinterview.ai").orElseThrow();
    }

    private SkillGapAnalysis createGap(User user, String matched, String partial, String missing) {
        Resume resume = resumeRepository.save(Resume.builder()
                .user(user).fileName("resume.pdf").originalFileName("resume.pdf")
                .filePath("resumes/resume.pdf").fileSize(100L).contentType("application/pdf")
                .rawText("Engineering experience").build());
        JobDescription jd = jobDescriptionRepository.save(JobDescription.builder()
                .user(user).title("Backend Engineer").rawText("Backend engineering role").build());
        return skillGapRepository.save(SkillGapAnalysis.builder()
                .resume(resume).jobDescription(jd).matchPercentage(50.0)
                .matchedSkillsJson(matched).partialSkillsJson(partial).missingSkillsJson(missing)
                .recommendationsJson("[]").analyzedAt(LocalDateTime.now()).build());
    }

    private JsonNode generate(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/preparation-plan/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}