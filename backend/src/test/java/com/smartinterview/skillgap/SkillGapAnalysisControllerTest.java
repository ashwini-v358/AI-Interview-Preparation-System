package com.smartinterview.skillgap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinterview.entity.JobDescription;
import com.smartinterview.entity.JobDescriptionAnalysis;
import com.smartinterview.entity.Resume;
import com.smartinterview.entity.ResumeAnalysis;
import com.smartinterview.entity.User;
import com.smartinterview.repository.JobDescriptionAnalysisRepository;
import com.smartinterview.repository.JobDescriptionRepository;
import com.smartinterview.repository.ResumeAnalysisRepository;
import com.smartinterview.repository.ResumeRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class SkillGapAnalysisControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private ResumeAnalysisRepository resumeAnalysisRepository;
    @Autowired private JobDescriptionRepository jobDescriptionRepository;
    @Autowired private JobDescriptionAnalysisRepository jdAnalysisRepository;

    @Test
    void analyzesAndPersistsResumeToJobSkillMatch() throws Exception {
        User candidate = userRepository.findByEmail("candidate@smartinterview.ai").orElseThrow();
        Resume resume = createResume(candidate);
        JobDescription jobDescription = createJobDescription(candidate);
        String token = login("candidate@smartinterview.ai", "Candidate@123456");

        MvcResult result = mockMvc.perform(post("/api/v1/skill-gap/analyze")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(resume.getId(), jobDescription.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchPercentage").value(50.0))
                .andExpect(jsonPath("$.matchedSkills[0].skillName").value("Java"))
                .andExpect(jsonPath("$.partialSkills[0].skillName").value("Spring Boot framework"))
                .andExpect(jsonPath("$.missingSkills[0].skillName").value("Docker"))
                .andExpect(jsonPath("$.recommendations.length()").value(2))
                .andReturn();

        JsonNode analysis = objectMapper.readTree(result.getResponse().getContentAsString());
        mockMvc.perform(get("/api/v1/skill-gap/latest")
                        .param("resumeId", resume.getId().toString())
                        .param("jobDescriptionId", jobDescription.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId").value(analysis.get("analysisId").asLong()));
    }

    @Test
    void hidesResumeAndJobDescriptionOwnedByAnotherUser() throws Exception {
        User candidate = userRepository.findByEmail("candidate@smartinterview.ai").orElseThrow();
        Resume resume = createResume(candidate);
        JobDescription jobDescription = createJobDescription(candidate);
        String adminToken = login("admin@smartinterview.ai", "Admin@123456");

        mockMvc.perform(post("/api/v1/skill-gap/analyze")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(resume.getId(), jobDescription.getId())))
                .andExpect(status().isNotFound());
    }

    private Resume createResume(User user) {
        Resume resume = resumeRepository.save(Resume.builder()
                .user(user)
                .originalFileName("backend-resume.pdf")
                .fileName("backend-resume.pdf")
                .filePath("resumes/backend-resume.pdf")
                .fileSize(100L)
                .contentType("application/pdf")
                .rawText("Java and Spring Boot experience")
                .build());
        resumeAnalysisRepository.save(ResumeAnalysis.builder()
                .resume(resume)
                .extractedSkillsJson("[\"Java\",\"Spring Boot\",\"SQL\"]")
                .experienceYears(4.0)
                .summary("Backend engineer")
                .domainsJson("[]")
                .educationJson("[]")
                .rawAiResponse("{}")
                .build());
        return resume;
    }

    private JobDescription createJobDescription(User user) {
        JobDescription jobDescription = jobDescriptionRepository.save(JobDescription.builder()
                .user(user)
                .title("Java Backend Engineer")
                .rawText("Java Spring Boot backend role requiring Docker experience.")
                .build());
        jdAnalysisRepository.save(JobDescriptionAnalysis.builder()
                .jobDescription(jobDescription)
                .roleTitle("Java Backend Engineer")
                .requiredSkillsJson("[\"Java\",\"Spring Boot framework\",\"Docker\"]")
                .niceToHaveSkillsJson("[]")
                .minimumExperienceYears(3.0)
                .roleSummary("Backend role")
                .rawAiResponse("{}")
                .build());
        return jobDescription;
    }

    private String request(Long resumeId, Long jobDescriptionId) {
        return "{\"resumeId\":" + resumeId + ",\"jobDescriptionId\":" + jobDescriptionId + "}";
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