package com.smartinterview.interview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinterview.entity.JobDescription;
import com.smartinterview.entity.Resume;
import com.smartinterview.entity.User;
import com.smartinterview.repository.InterviewQuestionRepository;
import com.smartinterview.repository.JobDescriptionRepository;
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

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class InterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private JobDescriptionRepository jobDescriptionRepository;

    @Autowired
    private InterviewQuestionRepository questionRepository;

    @Test
    void startsPersonalizedSessionPersistsRubricAndEvaluatesAnswer() throws Exception {
        String token = login("candidate@smartinterview.ai", "Candidate@123456");
        String request = """
                {"sessionTitle":"Backend interview","targetRole":"Java Backend Developer",
                 "interviewType":"TECHNICAL","targetQuestionCount":3,"difficulty":"MEDIUM"}
                """;

        MvcResult started = mockMvc.perform(post("/api/v1/interview/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentQuestion.questionId", notNullValue()))
                .andExpect(jsonPath("$.targetRole").value("Java Backend Developer"))
                .andReturn();

        JsonNode startBody = objectMapper.readTree(started.getResponse().getContentAsString());
        long sessionId = startBody.get("sessionId").asLong();
        long questionId = startBody.at("/currentQuestion/questionId").asLong();
        String firstQuestion = startBody.at("/currentQuestion/questionText").asText();
        assertTrue(firstQuestion.contains("Java Backend Developer"));
        assertFalse(questionRepository.findById(questionId).orElseThrow()
                .getExpectedKeyPointsJson().isBlank());

        MvcResult answered = mockMvc.perform(post("/api/v1/interview/{id}/answer/{questionId}", sessionId, questionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerText\":\"I used Spring Boot and React to build the application.\",\"timeTakenSeconds\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedQuestionCount").value(1))
                .andExpect(jsonPath("$.currentQuestion.questionId", notNullValue()))
                .andReturn();

        JsonNode answerBody = objectMapper.readTree(answered.getResponse().getContentAsString());
        JsonNode evaluation = objectMapper.readTree(answerBody.at("/history/0/detailedFeedback").asText());
        assertTrue(evaluation.has("relevanceScore"));
        assertTrue(evaluation.has("missingPoints"));
        assertTrue(evaluation.has("followUpNeeded"));
        assertTrue(answerBody.at("/currentQuestion/followUp").isMissingNode()
                || answerBody.at("/currentQuestion/questionText").asText().contains("I used Spring Boot"));
    }

    @Test
    void rejectsCrossUserResumeAndJdWhenStartingSession() throws Exception {
        User candidate = userRepository.findByEmail("candidate@smartinterview.ai").orElseThrow();
        Resume resume = resumeRepository.save(Resume.builder()
                .user(candidate)
                .fileName("candidate.pdf")
                .originalFileName("candidate.pdf")
                .filePath("resumes/candidate.pdf")
                .fileSize(10L)
                .contentType("application/pdf")
                .rawText("Built a Spring Boot service")
                .build());
        JobDescription jd = jobDescriptionRepository.save(JobDescription.builder()
                .user(candidate)
                .title("Java Backend Developer")
                .rawText("Java backend developer role requiring Spring Boot and SQL experience.")
                .build());
        String adminToken = login("admin@smartinterview.ai", "Admin@123456");

        mockMvc.perform(post("/api/v1/interview/start")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startRequest(resume.getId(), null)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/interview/start")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startRequest(null, jd.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidSessionAndMissingAnswer() throws Exception {
        String token = login("candidate@smartinterview.ai", "Candidate@123456");
        mockMvc.perform(get("/api/v1/interview/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        MvcResult started = mockMvc.perform(post("/api/v1/interview/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startRequest(null, null)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(started.getResponse().getContentAsString());

        mockMvc.perform(post("/api/v1/interview/{id}/answer/{questionId}",
                        body.get("sessionId").asLong(), body.at("/currentQuestion/questionId").asLong())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerText\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generatesAndRetrievesPersistedReportOnlyForSessionOwner() throws Exception {
        String token = login("candidate@smartinterview.ai", "Candidate@123456");
        MvcResult started = mockMvc.perform(post("/api/v1/interview/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startRequest(null, null)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode session = objectMapper.readTree(started.getResponse().getContentAsString());
        long sessionId = session.get("sessionId").asLong();
        long questionId = session.at("/currentQuestion/questionId").asLong();

        for (int order = 1; order <= 3; order++) {
            MvcResult submitted = mockMvc.perform(post("/api/v1/interview/{id}/answer/{questionId}", sessionId, questionId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"answerText\":\"Spring Boot exposes REST endpoints, JWT protects requests, and MySQL stores interview answers with indexes for queries.\"}"))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode response = objectMapper.readTree(submitted.getResponse().getContentAsString());
            if (order < 3) questionId = response.at("/currentQuestion/questionId").asLong();
            else assertEquals("COMPLETED", response.get("status").asText());
        }

        MvcResult generated = mockMvc.perform(post("/api/v1/interview/{id}/report", sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").isNumber())
                .andExpect(jsonPath("$.strengths").isArray())
                .andExpect(jsonPath("$.recommendedTopics").isArray())
                .andReturn();
        JsonNode report = objectMapper.readTree(generated.getResponse().getContentAsString());
        long reportId = report.get("reportId").asLong();

        mockMvc.perform(post("/api/v1/interview/{id}/report", sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(reportId));
        mockMvc.perform(get("/api/v1/interview/{id}/report", sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(reportId));

        String adminToken = login("admin@smartinterview.ai", "Admin@123456");
        mockMvc.perform(get("/api/v1/interview/{id}/report", sessionId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String startRequest(Long resumeId, Long jdId) throws Exception {
        var request = objectMapper.createObjectNode()
                .put("sessionTitle", "Security interview")
                .put("targetRole", "Java Backend Developer")
                .put("targetQuestionCount", 3);
        if (resumeId != null) request.put("resumeId", resumeId);
        if (jdId != null) request.put("jobDescriptionId", jdId);
        return objectMapper.writeValueAsString(request);
    }
}
