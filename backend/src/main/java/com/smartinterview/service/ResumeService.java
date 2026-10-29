package com.smartinterview.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinterview.ai.AIService;
import com.smartinterview.ai.ResumeAnalysisResult;
import com.smartinterview.dto.resume.ResumeUploadResponse;
import com.smartinterview.dto.resume.ResumeSummaryResponse;
import com.smartinterview.entity.Resume;
import com.smartinterview.entity.ResumeAnalysis;
import com.smartinterview.entity.User;
import com.smartinterview.exception.ResourceNotFoundException;
import com.smartinterview.repository.ResumeAnalysisRepository;
import com.smartinterview.repository.ResumeRepository;
import com.smartinterview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ResumeService — orchestrates the full resume processing pipeline:
 *
 *   1. Store the uploaded PDF to disk via FileStorageService.
 *   2. Extract raw text from the PDF via PdfExtractionService.
 *   3. Persist the Resume entity with metadata + raw text.
 *   4. Send raw text to AIService for structured analysis.
 *   5. Persist the ResumeAnalysis entity with AI results.
 *   6. Return a complete ResumeUploadResponse DTO.
 *
 * All steps run in a single @Transactional boundary so that if
 * AI analysis fails, no partial Resume record is committed to the DB.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final PdfExtractionService pdfExtractionService;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.provider:mock}")
    private String aiProvider;

    /**
     * Full upload pipeline: save → extract → persist → analyse → persist → respond.
     */
    @Transactional
    public ResumeUploadResponse uploadAndAnalyze(MultipartFile file, String userEmail) {
        long startTime = System.currentTimeMillis();

        // 1. Load authenticated user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        // 2. Store file to disk
        log.info("Storing resume PDF for user '{}'", userEmail);
        String storedPath = fileStorageService.storeFile(file, "resumes");

        // 3. Extract text from PDF
        log.info("Extracting text from PDF '{}'", file.getOriginalFilename());
        String rawText = pdfExtractionService.extractText(file);

        // 4. Persist Resume entity
        Resume resume = Resume.builder()
                .user(user)
                .originalFileName(file.getOriginalFilename())
                .fileName(storedPath.substring(storedPath.lastIndexOf('/') + 1))
                .filePath(storedPath)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .rawText(rawText)
                .build();
        resume = resumeRepository.save(resume);
        log.info("Resume saved with id={}", resume.getId());

        // 5. AI Analysis
        log.info("Sending resume text to AI service (provider={})", aiProvider);
        ResumeAnalysisResult aiResult = aiService.analyzeResume(rawText);

        // 6. Persist ResumeAnalysis entity
        String skillsJson = toJson(aiResult.extractedSkills());
        String domainsJson = toJson(aiResult.domains());
        String educationJson = toJson(aiResult.educationDetails());

        ResumeAnalysis analysis = ResumeAnalysis.builder()
                .resume(resume)
                .extractedSkillsJson(skillsJson)
                .experienceYears(aiResult.experienceYears())
                .summary(aiResult.summary())
                .domainsJson(domainsJson)
                .educationJson(educationJson)
                .rawAiResponse(aiResult.rawAiResponse())
                .analyzedAt(LocalDateTime.now())
                .build();
        analysis = resumeAnalysisRepository.save(analysis);
        log.info("ResumeAnalysis saved with id={}", analysis.getId());

        long processingMs = System.currentTimeMillis() - startTime;
        log.info("Resume processing complete in {}ms", processingMs);

        // 7. Build response DTO
        return ResumeUploadResponse.builder()
                .resumeId(resume.getId())
                .originalFileName(resume.getOriginalFileName())
                .fileSizeBytes(resume.getFileSize())
                .extractedTextLength(rawText.length())
                .uploadedAt(resume.getCreatedAt())
                .analysisId(analysis.getId())
                .extractedSkills(aiResult.extractedSkills())
                .experienceYears(aiResult.experienceYears())
                .summary(aiResult.summary())
                .domains(aiResult.domains())
                .educationDetails(aiResult.educationDetails())
                .aiProvider(aiProvider)
                .processingTimeMs(processingMs)
                .message("Resume uploaded and analysed successfully.")
                .build();
    }

    /**
     * Lists all resumes uploaded by the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<ResumeSummaryResponse> getMyResumes(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        return resumeRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * Fetches detailed analysis for a specific resume (owned by user).
     */
    @Transactional(readOnly = true)
    public ResumeUploadResponse getResumeDetail(Long resumeId, String userEmail) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));

        if (!resume.getUser().getEmail().equals(userEmail)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not own this resume");
        }

        ResumeAnalysis analysis = resumeAnalysisRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found for resume " + resumeId));

        return ResumeUploadResponse.builder()
                .resumeId(resume.getId())
                .originalFileName(resume.getOriginalFileName())
                .fileSizeBytes(resume.getFileSize())
                .extractedTextLength(resume.getRawText() != null ? resume.getRawText().length() : 0)
                .uploadedAt(resume.getCreatedAt())
                .analysisId(analysis.getId())
                .extractedSkills(fromJson(analysis.getExtractedSkillsJson()))
                .experienceYears(analysis.getExperienceYears() != null ? analysis.getExperienceYears() : 0)
                .summary(analysis.getSummary())
                .domains(fromJson(analysis.getDomainsJson()))
                .educationDetails(fromJson(analysis.getEducationJson()))
                .aiProvider(aiProvider)
                .processingTimeMs(0)
                .message("Resume detail retrieved.")
                .build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ResumeSummaryResponse toSummary(Resume resume) {
        boolean hasAnalysis = resume.getAnalysis() != null;
        List<String> skills = List.of();
        double experience = 0;
        String summary = null;

        if (hasAnalysis) {
            skills = fromJson(resume.getAnalysis().getExtractedSkillsJson())
                    .stream().limit(5).toList();
            experience = resume.getAnalysis().getExperienceYears() != null
                    ? resume.getAnalysis().getExperienceYears() : 0;
            summary = resume.getAnalysis().getSummary();
        }

        return ResumeSummaryResponse.builder()
                .resumeId(resume.getId())
                .originalFileName(resume.getOriginalFileName())
                .fileSizeBytes(resume.getFileSize())
                .uploadedAt(resume.getCreatedAt())
                .hasAnalysis(hasAnalysis)
                .topSkills(skills)
                .experienceYears(experience)
                .summary(summary)
                .build();
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "[]"; }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return List.of(); }
    }
}
