package com.smartinterview.readiness;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinterview.dto.readiness.ReadinessResponse;
import com.smartinterview.dto.skillgap.SkillGapAnalysisResponse.SkillItem;
import com.smartinterview.entity.InterviewReport;
import com.smartinterview.entity.InterviewSession;
import com.smartinterview.entity.SkillGapAnalysis;
import com.smartinterview.entity.User;
import com.smartinterview.exception.ResourceNotFoundException;
import com.smartinterview.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReadinessService {

    private final UserRepository userRepository;
    private final SkillGapAnalysisRepository skillGapRepository;
    private final PreparationPlanRepository preparationPlanRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewReportRepository interviewReportRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ReadinessResponse getReadiness(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        if (user.getRole() != null && user.getRole().name().equals("ROLE_ADMIN")) {
            return emptyResponse();
        }

        Optional<SkillGapAnalysis> skillGapOpt = skillGapRepository.findFirstByResumeUserIdOrderByAnalyzedAtDesc(user.getId());
        if (skillGapOpt.isEmpty()) {
            return emptyResponse();
        }

        SkillGapAnalysis skillGap = skillGapOpt.get();
        List<SkillItem> matched = readItems(skillGap.getMatchedSkillsJson());
        List<SkillItem> partial = readItems(skillGap.getPartialSkillsJson());
        List<SkillItem> missing = readItems(skillGap.getMissingSkillsJson());

        List<ReadinessResponse.ReadinessSkill> strongSkills = new ArrayList<>();
        matched.forEach(item -> strongSkills.add(new ReadinessResponse.ReadinessSkill(
                item.skillName(), 85, item.resumeEvidence() != null ? "Demonstrated in resume" : "Strong match", true, "HIGH", item.resumeEvidence())));

        List<ReadinessResponse.ReadinessSkill> improvementAreas = new ArrayList<>();
        partial.forEach(item -> improvementAreas.add(new ReadinessResponse.ReadinessSkill(
                item.skillName(), 65, item.suggestedPreparation() != null ? item.suggestedPreparation() : "Needs more practice", true, "MEDIUM", item.resumeEvidence())));
        missing.forEach(item -> improvementAreas.add(new ReadinessResponse.ReadinessSkill(
                item.skillName(), 40, item.suggestedPreparation() != null ? item.suggestedPreparation() : "Missing skill - needs to learn", true, "HIGH", null)));

        List<ReadinessResponse.ReadinessSkill> criticalSkills = new ArrayList<>();
        missing.forEach(item -> criticalSkills.add(new ReadinessResponse.ReadinessSkill(
                item.skillName(), 40, "Critical gap", true, "HIGH", null)));

        double interviewPerf = 0;
        List<InterviewSession> sessions = interviewSessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (!sessions.isEmpty()) {
            InterviewSession session = sessions.get(0);
            Optional<InterviewReport> reportOpt = interviewReportRepository.findByInterviewSessionId(session.getId());
            if (reportOpt.isPresent() && reportOpt.get().getOverallScore() != null) {
                interviewPerf = reportOpt.get().getOverallScore();
            }
        }

        int overall = (int) Math.round((skillGap.getMatchPercentage() * 0.4) + (interviewPerf * 0.4) + (strongSkills.size() * 10 * 0.2));
        if (overall < 0) overall = 0;
        if (overall > 100) overall = 100;

        List<ReadinessResponse.ReadinessGoal> dailyGoals = new ArrayList<>();
        if (!improvementAreas.isEmpty()) {
            dailyGoals.add(new ReadinessResponse.ReadinessGoal(
                    "Focus on " + improvementAreas.get(0).skillName(),
                    "Practice " + improvementAreas.get(0).skillName() + " with hands-on exercises",
                    "1-2 hours",
                    List.of("Review core concepts", "Complete practice exercises", "Apply in a small project")));
        }
        dailyGoals.add(new ReadinessResponse.ReadinessGoal(
                "Mock Interview Practice",
                "Practice answering common interview questions",
                "30-45 min",
                List.of("Prepare STAR method answers", "Practice technical questions", "Record and review")));

        List<ReadinessResponse.ScoreBreakdown> breakdown = List.of(
                new ReadinessResponse.ScoreBreakdown("Technical Skills", (int) (skillGap.getMatchPercentage() * 0.8), "Based on skill gap analysis"),
                new ReadinessResponse.ScoreBreakdown("Interview Performance", (int) interviewPerf, "Based on recent interviews"),
                new ReadinessResponse.ScoreBreakdown("Preparation Progress", 70, "Based on preparation activities")
        );

        return new ReadinessResponse(
                overall,
                (int) (skillGap.getMatchPercentage() * 0.9),
                65,
                70,
                75,
                (int) interviewPerf,
                60,
                strongSkills,
                improvementAreas,
                criticalSkills,
                dailyGoals,
                List.of(new ReadinessResponse.ReadinessGoal("Study Plan", "Follow structured learning path", "Ongoing", List.of("Complete daily goals"))),
                List.of(new ReadinessResponse.ReadinessGoal("Practice Exercises", "Hands-on coding practice", "30 min", List.of("Solve problems"))),
                List.of(new ReadinessResponse.ReadinessGoal("Interview Questions", "Practice common questions", "20 min", List.of("Review and answer"))),
                List.of("Review daily goals", "Practice for 30 min", "Update progress"),
                improvementAreas.isEmpty() ? "Keep up the great work!" : "Focus on improving " + improvementAreas.get(0).skillName(),
                "Readiness calculated based on your skill gap analysis and interview performance",
                breakdown
        );
    }

    @Transactional
    public ReadinessResponse refresh(String userEmail) {
        return getReadiness(userEmail);
    }

    private ReadinessResponse emptyResponse() {
        return new ReadinessResponse(
                0, 0, 0, 0, 0, 0, 0,
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(),
                List.of(),
                "Complete your skill gap analysis to get personalized recommendations",
                "No data available yet",
                List.of()
        );
    }

    private List<SkillItem> readItems(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception exception) {
            return List.of();
        }
    }
}
