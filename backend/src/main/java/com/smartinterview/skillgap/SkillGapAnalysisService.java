package com.smartinterview.skillgap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinterview.dto.skillgap.SkillGapAnalysisRequest;
import com.smartinterview.dto.skillgap.SkillGapAnalysisResponse;
import com.smartinterview.dto.skillgap.SkillGapAnalysisResponse.SkillItem;
import com.smartinterview.entity.JobDescription;
import com.smartinterview.entity.JobDescriptionAnalysis;
import com.smartinterview.entity.Resume;
import com.smartinterview.entity.ResumeAnalysis;
import com.smartinterview.entity.SkillGapAnalysis;
import com.smartinterview.entity.User;
import com.smartinterview.exception.ResourceNotFoundException;
import com.smartinterview.repository.JobDescriptionAnalysisRepository;
import com.smartinterview.repository.JobDescriptionRepository;
import com.smartinterview.repository.ResumeAnalysisRepository;
import com.smartinterview.repository.ResumeRepository;
import com.smartinterview.repository.SkillGapAnalysisRepository;
import com.smartinterview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SkillGapAnalysisService {

    private final SkillGapAnalysisRepository skillGapRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final JobDescriptionAnalysisRepository jobDescriptionAnalysisRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public SkillGapAnalysisResponse analyze(SkillGapAnalysisRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        Resume resume = resumeRepository.findByIdAndUserId(request.resumeId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume", request.resumeId()));
        JobDescription jobDescription = jobDescriptionRepository
                .findByIdAndUserId(request.jobDescriptionId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("JobDescription", request.jobDescriptionId()));
        ResumeAnalysis resumeAnalysis = resumeAnalysisRepository.findByResumeId(resume.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found for resume " + resume.getId()));
        JobDescriptionAnalysis jdAnalysis = jobDescriptionAnalysisRepository
                .findByJobDescriptionId(jobDescription.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found for job description " + jobDescription.getId()));

        List<String> resumeSkills = fromJson(resumeAnalysis.getExtractedSkillsJson());
        List<String> requiredSkills = fromJson(jdAnalysis.getRequiredSkillsJson());
        List<SkillItem> matched = new ArrayList<>();
        List<SkillItem> partial = new ArrayList<>();
        List<SkillItem> missing = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        double weightedMatches = 0;

        for (String requiredSkill : requiredSkills) {
            String exactEvidence = resumeSkills.stream()
                    .filter(skill -> normalize(skill).equals(normalize(requiredSkill)))
                    .findFirst().orElse(null);
            if (exactEvidence != null) {
                matched.add(new SkillItem(requiredSkill, exactEvidence, requiredSkill, null));
                weightedMatches += 1;
                continue;
            }

            String partialEvidence = resumeSkills.stream()
                    .filter(skill -> isPartialMatch(skill, requiredSkill))
                    .findFirst().orElse(null);
            if (partialEvidence != null) {
                String suggestion = "Make your experience with " + requiredSkill
                        + " more explicit by naming the tools used and the result.";
                partial.add(new SkillItem(requiredSkill, partialEvidence, requiredSkill, suggestion));
                recommendations.add(suggestion);
                weightedMatches += 0.5;
            } else {
                String suggestion = "Add hands-on evidence for " + requiredSkill
                        + ", such as a project, measurable outcome, or practical example.";
                missing.add(new SkillItem(requiredSkill, null, requiredSkill, suggestion));
                recommendations.add(suggestion);
            }
        }

        if (requiredSkills.isEmpty()) {
            recommendations.add("No required skills were extracted. Review the job description and try again with its full requirements.");
        }
        double matchPercentage = requiredSkills.isEmpty() ? 0 : weightedMatches * 100 / requiredSkills.size();
        SkillGapAnalysis analysis = skillGapRepository
                .findFirstByResumeIdAndJobDescriptionIdOrderByCreatedAtDesc(resume.getId(), jobDescription.getId())
                .orElseGet(() -> SkillGapAnalysis.builder().resume(resume).jobDescription(jobDescription).build());
        analysis.setMatchPercentage(matchPercentage);
        analysis.setMatchedSkillsJson(toJson(matched));
        analysis.setPartialSkillsJson(toJson(partial));
        analysis.setMissingSkillsJson(toJson(missing));
        analysis.setRecommendationsJson(toJson(recommendations));
        analysis.setAnalyzedAt(LocalDateTime.now());
        SkillGapAnalysis saved = skillGapRepository.save(analysis);
        return toResponse(saved, resume, jobDescription);
    }

    @Transactional(readOnly = true)
    public SkillGapAnalysisResponse getLatest(Long resumeId, Long jobDescriptionId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));
        JobDescription jobDescription = jobDescriptionRepository.findByIdAndUserId(jobDescriptionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("JobDescription", jobDescriptionId));
        SkillGapAnalysis analysis = skillGapRepository
                .findFirstByResumeIdAndJobDescriptionIdOrderByCreatedAtDesc(resume.getId(), jobDescription.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill gap analysis not found"));
        return toResponse(analysis, resume, jobDescription);
    }

    private SkillGapAnalysisResponse toResponse(SkillGapAnalysis analysis, Resume resume, JobDescription jobDescription) {
        return new SkillGapAnalysisResponse(
                analysis.getId(), resume.getId(), resume.getOriginalFileName(), jobDescription.getId(),
                jobDescription.getTitle(), analysis.getMatchPercentage(),
                fromJson(analysis.getMatchedSkillsJson(), new TypeReference<>() {}),
                fromJson(analysis.getPartialSkillsJson(), new TypeReference<>() {}),
                fromJson(analysis.getMissingSkillsJson(), new TypeReference<>() {}),
                fromJson(analysis.getRecommendationsJson(), new TypeReference<>() {}),
                analysis.getAnalyzedAt());
    }

    private boolean isPartialMatch(String resumeSkill, String requiredSkill) {
        Set<String> evidenceTokens = tokens(resumeSkill);
        Set<String> requiredTokens = tokens(requiredSkill);
        if (evidenceTokens.isEmpty() || requiredTokens.isEmpty()) return false;
        long overlappingTokens = requiredTokens.stream().filter(evidenceTokens::contains).count();
        return (double) overlappingTokens / requiredTokens.size() >= 0.5;
    }

    private Set<String> tokens(String value) {
                return Arrays.stream(normalize(value).split(" "))
                .filter(token -> token.length() > 1)
                .collect(Collectors.toSet());
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9+#.]+", " ").trim().replaceAll("\\s+", " ");
    }

    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException("Could not persist skill gap analysis", exception); }
    }

    private List<String> fromJson(String json) {
        return fromJson(json, new TypeReference<>() {});
    }

    private <T> List<T> fromJson(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, type); }
        catch (Exception exception) { return List.of(); }
    }
}