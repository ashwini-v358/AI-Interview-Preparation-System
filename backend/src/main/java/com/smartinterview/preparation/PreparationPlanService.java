package com.smartinterview.preparation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinterview.dto.preparation.PreparationPlanResponse;
import com.smartinterview.dto.preparation.PreparationPlanResponse.SkillPlanItem;
import com.smartinterview.dto.skillgap.SkillGapAnalysisResponse.SkillItem;
import com.smartinterview.entity.PreparationPlan;
import com.smartinterview.entity.SkillGapAnalysis;
import com.smartinterview.entity.User;
import com.smartinterview.exception.ResourceNotFoundException;
import com.smartinterview.repository.PreparationPlanRepository;
import com.smartinterview.repository.SkillGapAnalysisRepository;
import com.smartinterview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PreparationPlanService {

    private final PreparationPlanRepository planRepository;
    private final SkillGapAnalysisRepository skillGapRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public PreparationPlanResponse generate(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        SkillGapAnalysis skillGap = skillGapRepository.findFirstByResumeUserIdOrderByAnalyzedAtDesc(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Run a skill-gap analysis before generating a preparation plan"));

        List<SkillItem> matched = readItems(skillGap.getMatchedSkillsJson());
        List<SkillItem> partial = readItems(skillGap.getPartialSkillsJson());
        List<SkillItem> missing = readItems(skillGap.getMissingSkillsJson());
        List<SkillPlanItem> skillAreas = new ArrayList<>();
        partial.forEach(item -> skillAreas.add(toPlanItem(item, "WEAK", "MEDIUM", 2, 3)));
        missing.forEach(item -> skillAreas.add(toPlanItem(item, "MISSING", "HIGH", 4, 6)));

        int durationDays = skillAreas.stream().mapToInt(SkillPlanItem::estimatedDays).sum();
        String title = "Preparation Plan: " + skillGap.getJobDescription().getTitle();
        String content = writeContent(skillGap, matched.stream().map(SkillItem::skillName).toList(), skillAreas);
        PreparationPlan plan = planRepository.save(PreparationPlan.builder()
                .user(user)
                .title(title)
                .planContentJson(content)
                .durationDays(durationDays)
                .build());
        return toResponse(plan);
    }

    @Transactional(readOnly = true)
    public PreparationPlanResponse getCurrent(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        PreparationPlan plan = planRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Preparation plan not found"));
        return toResponse(plan);
    }

    private SkillPlanItem toPlanItem(SkillItem skill, String status, String priority,
                                     int estimatedDays, int estimatedHours) {
        String name = skill.skillName();
        List<String> topics = status.equals("MISSING")
                ? List.of("Core concepts and terminology for " + name,
                        "Common tools and practical workflows in " + name,
                        "Trade-offs, limitations, and production use cases")
                : List.of("Deepen your understanding of " + name,
                        "Practice common patterns and troubleshooting in " + name,
                        "Prepare clear examples of your hands-on experience");
        List<String> tasks = status.equals("MISSING")
                ? List.of("Complete a small hands-on exercise using " + name,
                        "Document the problem, approach, and outcome in a short project note")
                : List.of("Build or extend a small example that demonstrates " + name,
                        "Write a concise explanation of your decisions and results");
        List<String> interviewSuggestions = List.of(
                "Practice explaining " + name + " in a two-minute answer",
                "Prepare one scenario question and discuss alternatives and trade-offs");
        return new SkillPlanItem(name, status, priority, estimatedDays, estimatedHours,
                topics, tasks, interviewSuggestions);
    }

    private String writeContent(SkillGapAnalysis skillGap, List<String> strongSkills,
                                List<SkillPlanItem> skillAreas) {
        PlanContent content = new PlanContent(skillGap.getId(), skillGap.getMatchPercentage(),
                skillGap.getResume().getOriginalFileName(), skillGap.getJobDescription().getTitle(),
                strongSkills, skillAreas);
        try {
            return objectMapper.writeValueAsString(content);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize preparation plan", exception);
        }
    }

    private PreparationPlanResponse toResponse(PreparationPlan plan) {
        try {
            PlanContent content = objectMapper.readValue(plan.getPlanContentJson(), PlanContent.class);
            return new PreparationPlanResponse(plan.getId(), plan.getTitle(), plan.getDurationDays(),
                    plan.getCreatedAt(), content.skillGapAnalysisId(), content.resumeFileName(),
                    content.jobTitle(), content.matchPercentage(), content.strongSkills(), content.skillAreas());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read saved preparation plan", exception);
        }
    }

    private List<SkillItem> readItems(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read saved skill-gap results", exception);
        }
    }

    private record PlanContent(Long skillGapAnalysisId, double matchPercentage,
                               String resumeFileName, String jobTitle,
                               List<String> strongSkills, List<SkillPlanItem> skillAreas) {}
}