package com.smartinterview.jd;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinterview.ai.AIService;
import com.smartinterview.ai.JobDescriptionAnalysisResult;
import com.smartinterview.dto.jd.JdSummaryResponse;
import com.smartinterview.dto.jd.JdUploadRequest;
import com.smartinterview.dto.jd.JdUploadResponse;
import com.smartinterview.entity.JobDescription;
import com.smartinterview.entity.JobDescriptionAnalysis;
import com.smartinterview.entity.User;
import com.smartinterview.exception.ResourceNotFoundException;
import com.smartinterview.repository.JobDescriptionAnalysisRepository;
import com.smartinterview.repository.JobDescriptionRepository;
import com.smartinterview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JobDescriptionService — orchestrates the JD upload + AI analysis pipeline.
 *
 * Pipeline:
 *   1. Load authenticated user
 *   2. Persist JobDescription entity with raw text
 *   3. Send raw text to AIService for structured analysis
 *   4. Persist JobDescriptionAnalysis entity
 *   5. Return JdUploadResponse DTO
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobDescriptionService {

    private final JobDescriptionRepository jdRepository;
    private final JobDescriptionAnalysisRepository jdAnalysisRepository;
    private final UserRepository userRepository;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.provider:mock}")
    private String aiProvider;

    /**
     * Accepts a raw JD text, analyses it with AI, and persists both JD + analysis.
     */
    @Transactional
    public JdUploadResponse uploadAndAnalyze(JdUploadRequest request, String userEmail) {
        long startTime = System.currentTimeMillis();

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        // 1. Persist the JobDescription entity
        JobDescription jd = JobDescription.builder()
                .user(user)
                .title(request.getTitle())
                .companyName(request.getCompanyName())
                .rawText(request.getRawText())
                .build();
        jd = jdRepository.save(jd);
        log.info("JobDescription saved with id={}", jd.getId());

        // 2. AI Analysis
        log.info("Sending JD text to AI service (provider={})", aiProvider);
        JobDescriptionAnalysisResult aiResult = aiService.analyzeJobDescription(request.getRawText());

        // 3. Persist JobDescriptionAnalysis
        String requiredSkillsJson = toJson(aiResult.requiredSkills());
        String niceToHaveJson = toJson(aiResult.niceToHaveSkills());

        JobDescriptionAnalysis analysis = JobDescriptionAnalysis.builder()
                .jobDescription(jd)
                .roleTitle(aiResult.roleTitle())
                .requiredSkillsJson(requiredSkillsJson)
                .niceToHaveSkillsJson(niceToHaveJson)
                .minimumExperienceYears(aiResult.experienceRequired())
                .roleSummary("Role requiring " + aiResult.requiredSkills().size() + " core skills across " +
                        String.join(", ", aiResult.domains()))
                .rawAiResponse(aiResult.rawAiResponse())
                .analyzedAt(LocalDateTime.now())
                .build();
        analysis = jdAnalysisRepository.save(analysis);
        log.info("JobDescriptionAnalysis saved with id={}", analysis.getId());

        long processingMs = System.currentTimeMillis() - startTime;

        return JdUploadResponse.builder()
                .jdId(jd.getId())
                .title(jd.getTitle())
                .companyName(jd.getCompanyName())
                .rawTextLength(request.getRawText().length())
                .uploadedAt(jd.getCreatedAt())
                .analysisId(analysis.getId())
                .roleTitle(aiResult.roleTitle())
                .requiredSkills(aiResult.requiredSkills())
                .niceToHaveSkills(aiResult.niceToHaveSkills())
                .minimumExperienceYears(aiResult.experienceRequired())
                .domains(aiResult.domains())
                .aiProvider(aiProvider)
                .processingTimeMs(processingMs)
                .message("Job description uploaded and analysed successfully.")
                .build();
    }

    /**
     * Returns all JDs uploaded by the current user.
     */
    @Transactional(readOnly = true)
    public List<JdSummaryResponse> getMyJds(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        return jdRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * Returns full analysis detail for one JD.
     */
    @Transactional(readOnly = true)
    public JdUploadResponse getJdDetail(Long jdId, String userEmail) {
        JobDescription jd = jdRepository.findById(jdId)
                .orElseThrow(() -> new ResourceNotFoundException("JobDescription", jdId));
        if (!jd.getUser().getEmail().equals(userEmail)) {
            throw new org.springframework.security.access.AccessDeniedException("You do not own this JD");
        }
        JobDescriptionAnalysis analysis = jdAnalysisRepository.findByJobDescriptionId(jdId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found for JD " + jdId));

        return JdUploadResponse.builder()
                .jdId(jd.getId())
                .title(jd.getTitle())
                .companyName(jd.getCompanyName())
                .rawTextLength(jd.getRawText() != null ? jd.getRawText().length() : 0)
                .uploadedAt(jd.getCreatedAt())
                .analysisId(analysis.getId())
                .roleTitle(analysis.getRoleTitle())
                .requiredSkills(fromJson(analysis.getRequiredSkillsJson()))
                .niceToHaveSkills(fromJson(analysis.getNiceToHaveSkillsJson()))
                .minimumExperienceYears(analysis.getMinimumExperienceYears() != null ? analysis.getMinimumExperienceYears() : 0)
                .domains(List.of())
                .aiProvider(aiProvider)
                .processingTimeMs(0)
                .message("JD detail retrieved.")
                .build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private JdSummaryResponse toSummary(JobDescription jd) {
        boolean hasAnalysis = jd.getAnalysis() != null;
        List<String> topSkills = List.of();
        double minExp = 0;
        String roleTitle = null;

        if (hasAnalysis) {
            topSkills = fromJson(jd.getAnalysis().getRequiredSkillsJson()).stream().limit(5).toList();
            minExp = jd.getAnalysis().getMinimumExperienceYears() != null ? jd.getAnalysis().getMinimumExperienceYears() : 0;
            roleTitle = jd.getAnalysis().getRoleTitle();
        }

        return JdSummaryResponse.builder()
                .jdId(jd.getId())
                .title(jd.getTitle())
                .companyName(jd.getCompanyName())
                .uploadedAt(jd.getCreatedAt())
                .hasAnalysis(hasAnalysis)
                .topRequiredSkills(topSkills)
                .minimumExperienceYears(minExp)
                .roleTitle(roleTitle)
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
