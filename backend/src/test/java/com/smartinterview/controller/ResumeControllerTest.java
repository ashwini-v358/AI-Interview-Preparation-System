package com.smartinterview.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinterview.dto.auth.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String bearerToken;

    /**
     * Login as the seeded candidate to get a valid JWT before each test.
     */
    @BeforeEach
    void loginAndGetToken() throws Exception {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("candidate@smartinterview.ai");
        loginReq.setPassword("Candidate@123456");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        bearerToken = objectMapper.readTree(
                result.getResponse().getContentAsString()).get("token").asText();
    }

    // ─── Minimal PDF bytes ────────────────────────────────────────────────────
    // A minimal valid PDF so PDFBox doesn't throw while parsing the file header.
    private static final byte[] MINIMAL_PDF = (
            "%PDF-1.4\n" +
            "1 0 obj<</Type /Catalog /Pages 2 0 R>>endobj\n" +
            "2 0 obj<</Type /Pages /Kids[3 0 R] /Count 1>>endobj\n" +
            "3 0 obj<</Type /Page /Parent 2 0 R /MediaBox[0 0 612 792] /Contents 4 0 R /Resources<<>>>>\n" +
            "endobj\n" +
            "4 0 obj<</Length 44>>\n" +
            "stream\n" +
            "BT /F1 12 Tf 100 700 Td (Java Spring Boot React) Tj ET\n" +
            "endstream\n" +
            "endobj\n" +
            "xref\n" +
            "0 5\n" +
            "0000000000 65535 f\n" +
            "trailer<</Size 5 /Root 1 0 R>>\n" +
            "%%EOF"
    ).getBytes();

    @Test
    @DisplayName("Should upload a PDF and get AI analysis results (mock provider)")
    void testUploadResume() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "my_resume.pdf", "application/pdf", MINIMAL_PDF);

        mockMvc.perform(multipart("/api/v1/resume/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + bearerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resumeId", notNullValue()))
                .andExpect(jsonPath("$.originalFileName", is("my_resume.pdf")))
                .andExpect(jsonPath("$.extractedSkills", notNullValue()))
                .andExpect(jsonPath("$.aiProvider", is("mock")))
                .andExpect(jsonPath("$.message", containsString("successfully")));
    }

    @Test
    @DisplayName("Should return 401 when uploading without JWT")
    void testUploadRequiresAuth() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", MINIMAL_PDF);

        mockMvc.perform(multipart("/api/v1/resume/upload").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject non-PDF files with 400")
    void testUploadRejectsNonPdf() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "not a pdf".getBytes());

        mockMvc.perform(multipart("/api/v1/resume/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + bearerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /my should return empty list for a fresh candidate")
    void testGetMyResumesEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/resume/my")
                        .header("Authorization", "Bearer " + bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    @DisplayName("GET /my should list uploaded resumes after upload")
    void testGetMyResumesAfterUpload() throws Exception {
        // Upload first
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", MINIMAL_PDF);
        mockMvc.perform(multipart("/api/v1/resume/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + bearerToken))
                .andExpect(status().isCreated());

        // List
        mockMvc.perform(get("/api/v1/resume/my")
                        .header("Authorization", "Bearer " + bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].originalFileName", is("resume.pdf")));
    }
}
