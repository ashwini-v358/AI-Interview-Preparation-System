package com.smartinterview.interview;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinterview.ai.InterviewSkillSignal;
import com.smartinterview.dto.interview.AdaptiveProfileResponse;
import com.smartinterview.dto.interview.AdaptiveProfileResponse.InterviewTrend;
import com.smartinterview.dto.interview.AdaptiveProfileResponse.SkillPerformance;
import com.smartinterview.dto.interview.AdaptiveReasonDto;
import com.smartinterview.dto.skillgap.SkillGapAnalysisResponse.SkillItem;
import com.smartinterview.entity.*;
import com.smartinterview.entity.enums.DifficultyLevel;
import com.smartinterview.entity.enums.InterviewStatus;
import com.smartinterview.exception.ResourceNotFoundException;
import com.smartinterview.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewEvolutionService {

    private static final int HISTORY_LIMIT = 10;

    private final UserRepository userRepository;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final SkillGapAnalysisRepository skillGapRepository;
    private final PreparationPlanRepository preparationPlanRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public AdaptiveProfileResponse getProfile(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        return buildProfile(user.getId(), null);
    }

    /**
     * Reads the candidate-specific signals the AI needs to ground a generated question:
     * detected skill gaps, previous interview performance, and preparation-plan coverage.
     *
     * All lookups are scoped to {@code session.getUser()}, so only the owning candidate's own
     * skill gaps, interview history and preparation plan can ever reach the AI prompt.
     * Empty lists are returned when the candidate has no such records yet.
     */
    @Transactional(readOnly = true)
    public InterviewContextSignals loadContextSignals(InterviewSession session) {
        User user = session.getUser();
        if (user == null) return InterviewContextSignals.empty();

        List<InterviewSkillSignal> performance = toSkillPerformance(
                        readPerformance(user.getId(), session.getId())).stream()
                .map(item -> new InterviewSkillSignal(item.skillName(), item.level(),
                        item.scorePercent(), "Previous interview average " + Math.round(item.scorePercent()) + "%"))
                .toList();

        SkillGapAnalysis skillGap = findRelevantSkillGap(session, user.getId());
        List<InterviewSkillSignal> skillGaps = skillGap == null ? List.of() : toGapSignals(skillGap);

        List<InterviewSkillSignal> preparation = skillGap == null
                ? List.of() : toPreparationSignals(user.getId(), skillGap.getId());
        return new InterviewContextSignals(skillGaps, performance, preparation);
    }

    private List<InterviewSkillSignal> toGapSignals(SkillGapAnalysis skillGap) {
        List<InterviewSkillSignal> signals = new ArrayList<>();
        readSkillItems(skillGap.getMatchedSkillsJson()).forEach(item ->
                signals.add(new InterviewSkillSignal(item.skillName(), "MATCHED", null, gapEvidence(item))));
        readSkillItems(skillGap.getPartialSkillsJson()).forEach(item ->
                signals.add(new InterviewSkillSignal(item.skillName(), "WEAK", null, gapEvidence(item))));
        readSkillItems(skillGap.getMissingSkillsJson()).forEach(item ->
                signals.add(new InterviewSkillSignal(item.skillName(), "MISSING", null, gapEvidence(item))));
        return signals;
    }

    private String gapEvidence(SkillItem item) {
        StringBuilder evidence = new StringBuilder();
        if (item.resumeEvidence() != null && !item.resumeEvidence().isBlank()) {
            evidence.append("resume: ").append(item.resumeEvidence());
        }
        if (item.jdRequirement() != null && !item.jdRequirement().isBlank()) {
            if (!evidence.isEmpty()) evidence.append(" | ");
            evidence.append("job: ").append(item.jdRequirement());
        }
        return evidence.isEmpty() ? null : evidence.toString();
    }

    private List<InterviewSkillSignal> toPreparationSignals(Long userId, Long skillGapId) {
        return preparationPlanRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(userId)
                .map(plan -> readPlanSkillAreas(plan.getPlanContentJson(), skillGapId))
                .orElse(List.of());
    }

    private List<InterviewSkillSignal> readPlanSkillAreas(String json, Long skillGapId) {
        try {
            JsonNode content = objectMapper.readTree(json);
            if (content.path("skillGapAnalysisId").asLong(-1) != skillGapId) return List.of();
            List<InterviewSkillSignal> signals = new ArrayList<>();
            for (JsonNode item : content.path("skillAreas")) {
                String skill = item.path("skillName").asText("");
                if (skill.isBlank()) continue;
                String status = item.path("currentStatus").asText("");
                String priority = item.path("priority").asText("");
                signals.add(new InterviewSkillSignal(skill,
                        status.isBlank() ? "IN_PREPARATION" : status, null,
                        priority.isBlank() ? "In the current preparation plan" : "Preparation priority " + priority));
            }
            return signals;
        } catch (Exception exception) {
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public AdaptiveQuestionPlan selectQuestion(InterviewSession session, int questionOrder) {
        User user = session.getUser();
        Map<String, SkillAccumulator> performance = readPerformance(user.getId(), session.getId());
        List<SkillPerformance> profile = toSkillPerformance(performance);
        Map<String, SkillPerformance> bySkill = profile.stream()
                .collect(Collectors.toMap(item -> normalize(item.skillName()), item -> item, (a, b) -> a));

        List<String> requiredSkills = session.getJobDescription() != null
                && session.getJobDescription().getAnalysis() != null
                ? readStringList(session.getJobDescription().getAnalysis().getRequiredSkillsJson()) : List.of();
        SkillGapAnalysis skillGap = findRelevantSkillGap(session, user.getId());
        Map<String, String> gapTypes = readGapTypes(skillGap);
        Set<String> planSkills = findCurrentPlanSkills(user.getId(), skillGap == null ? null : skillGap.getId());

        LinkedHashMap<String, SkillCandidate> candidates = new LinkedHashMap<>();
        requiredSkills.forEach(skill -> addCandidate(candidates, skill));
        profile.forEach(skill -> addCandidate(candidates, skill.skillName()));
        gapTypes.keySet().forEach(skill -> addCandidate(candidates, skill));
        planSkills.forEach(skill -> addCandidate(candidates, skill));
        addSessionSkills(candidates, session);

        if (candidates.isEmpty()) {
            return new AdaptiveQuestionPlan(null, session.getCurrentDifficulty(), List.of());
        }

        List<WeightedSkill> weighted = candidates.values().stream().map(candidate -> {
            String normalized = normalize(candidate.name);
            SkillPerformance score = bySkill.get(normalized);
            String gapType = gapTypes.get(normalized);
            boolean required = containsSkill(requiredSkills, candidate.name);
            boolean prepared = containsSkill(planSkills, candidate.name);
            int mistakes = score == null ? 0 : score.previousMistakes();
            double weight = 1.0 + (required ? 0.5 : 0.0) + (prepared ? 0.25 : 0.0);
            if ("MISSING".equals(gapType)) weight += 1.5;
            else if ("WEAK".equals(gapType)) weight += 0.75;
            if (score != null && score.scorePercent() <= 40) weight += 1.5;
            else if (score != null && score.scorePercent() <= 70) weight += 0.75;
            weight += Math.min(mistakes, 2) * 0.5;
            return new WeightedSkill(candidate.name, weight, score, gapType, required, prepared);
        }).toList();

        String focusSkill = chooseWeightedSkill(weighted, questionOrder);
        WeightedSkill selected = weighted.stream()
                .filter(skill -> normalize(skill.name).equals(normalize(focusSkill)))
                .findFirst().orElseThrow();
        List<AdaptiveReasonDto> reasons = new ArrayList<>(reasonsFor(selected));
        DifficultyLevel difficulty = selected.performance == null
                ? session.getCurrentDifficulty() : difficultyFor(selected.performance.scorePercent());
        addSessionDifficultyReason(session, questionOrder, difficulty, reasons);
        return new AdaptiveQuestionPlan(focusSkill, difficulty, reasons);
    }

        private void addSessionDifficultyReason(InterviewSession session, int questionOrder,
                            DifficultyLevel difficulty, List<AdaptiveReasonDto> reasons) {
        if (questionOrder <= 1 || reasons.stream().anyMatch(reason ->
            reason.category().equals("DIFFICULTY_PROGRESSION"))) return;
        questionRepository.findByInterviewSessionIdOrderByQuestionOrderAsc(session.getId()).stream()
            .filter(question -> question.getQuestionOrder() < questionOrder)
            .sorted(Comparator.comparing(InterviewQuestion::getQuestionOrder).reversed())
            .filter(question -> question.getCandidateAnswer() != null
                && question.getCandidateAnswer().getEvaluation() != null)
            .filter(question -> difficulty == DifficultyLevel.EASY
                ? question.getCandidateAnswer().getEvaluation().getOverallScore() < 5.0
                : difficulty == DifficultyLevel.HARD
                && question.getCandidateAnswer().getEvaluation().getOverallScore() >= 8.0)
            .findFirst()
            .ifPresent(question -> {
                String skill = question.getTargetSkill() == null ? "the previous question" : question.getTargetSkill();
                double score = question.getCandidateAnswer().getEvaluation().getOverallScore();
                reasons.add(new AdaptiveReasonDto("DIFFICULTY_PROGRESSION", String.format(Locale.ROOT,
                    "Your previous %s answer scored %.1f/10, so this interview is using %s difficulty for reinforcement.",
                    skill, score, difficulty)));
            });
        }

    private AdaptiveProfileResponse buildProfile(Long userId, Long includeInProgressSessionId) {
        List<InterviewSession> completed = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(session -> session.getStatus() == InterviewStatus.COMPLETED)
                .sorted(Comparator.comparing(InterviewSession::getCompletedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(HISTORY_LIMIT)
                .toList();

        Map<String, SkillAccumulator> overall = new LinkedHashMap<>();
        List<InterviewTrend> trends = new ArrayList<>();
        for (InterviewSession session : completed) {
            Map<String, SkillAccumulator> sessionScores = readSessionPerformance(session.getId());
            sessionScores.forEach((key, value) -> overall.computeIfAbsent(key, ignored -> new SkillAccumulator(value.name))
                    .merge(value));
            double overallScore = sessionScores.values().stream()
                    .flatMap(accumulator -> accumulator.scores.stream()).mapToDouble(Double::doubleValue)
                    .average().orElse(0);
            trends.add(new InterviewTrend(session.getId(), session.getSessionTitle(), session.getCompletedAt(),
                    percent(overallScore), toSkillPerformance(sessionScores)));
        }
        if (includeInProgressSessionId != null) {
            readSessionPerformance(includeInProgressSessionId).forEach((key, value) ->
                    overall.computeIfAbsent(key, ignored -> new SkillAccumulator(value.name)).merge(value));
        }

        List<SkillPerformance> skills = toSkillPerformance(overall);
        String suggestedDifficulty = skills.isEmpty() ? DifficultyLevel.MEDIUM.name()
                : difficultyFor(skills.get(0).scorePercent()).name();
        long completedCount = sessionRepository.countByUserIdAndStatus(userId, InterviewStatus.COMPLETED);
        return new AdaptiveProfileResponse(completedCount > 0, Math.toIntExact(completedCount),
            suggestedDifficulty, skills, trends);
    }

    private Map<String, SkillAccumulator> readPerformance(Long userId, Long inProgressSessionId) {
        Map<String, SkillAccumulator> result = new LinkedHashMap<>();
        List<InterviewSession> completed = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(session -> session.getStatus() == InterviewStatus.COMPLETED)
                .sorted(Comparator.comparing(InterviewSession::getCompletedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(HISTORY_LIMIT).toList();
        completed.forEach(session -> readSessionPerformance(session.getId()).forEach((key, value) ->
                result.computeIfAbsent(key, ignored -> new SkillAccumulator(value.name)).merge(value)));
        if (inProgressSessionId != null) {
            readSessionPerformance(inProgressSessionId).forEach((key, value) ->
                    result.computeIfAbsent(key, ignored -> new SkillAccumulator(value.name)).merge(value));
        }
        return result;
    }

    private Map<String, SkillAccumulator> readSessionPerformance(Long sessionId) {
        Map<String, SkillAccumulator> result = new LinkedHashMap<>();
        for (InterviewQuestion question : questionRepository.findByInterviewSessionIdOrderByQuestionOrderAsc(sessionId)) {
            CandidateAnswer answer = question.getCandidateAnswer();
            AnswerEvaluation evaluation = answer == null ? null : answer.getEvaluation();
            if (evaluation == null || evaluation.getOverallScore() == null || question.getTargetSkill() == null
                    || question.getTargetSkill().isBlank()) continue;
            SkillAccumulator accumulator = result.computeIfAbsent(normalize(question.getTargetSkill()),
                    ignored -> new SkillAccumulator(question.getTargetSkill()));
            accumulator.add(evaluation);
        }
        return result;
    }

    private List<SkillPerformance> toSkillPerformance(Map<String, SkillAccumulator> accumulators) {
        return accumulators.values().stream().map(SkillAccumulator::toDto)
                .sorted(Comparator.comparingDouble(SkillPerformance::scorePercent)
                        .thenComparing(SkillPerformance::skillName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<AdaptiveReasonDto> reasonsFor(WeightedSkill skill) {
        List<AdaptiveReasonDto> reasons = new ArrayList<>();
        if (skill.gapType != null) {
            String explanation = "MISSING".equals(skill.gapType)
                    ? skill.name + " was not identified in your resume for this role."
                    : skill.name + " has only partial evidence in your resume for this role.";
            reasons.add(new AdaptiveReasonDto("SKILL_GAP", explanation));
        }
        if (skill.performance != null && skill.performance.previousMistakes() > 0) {
            reasons.add(new AdaptiveReasonDto("PREVIOUS_MISTAKE", "You previously scored below 5/10 on "
                    + skill.name + " in " + skill.performance.previousMistakes() + " answer(s)."));
        }
        if (skill.performance != null && skill.performance.scorePercent() <= 40) {
            reasons.add(new AdaptiveReasonDto("LOW_PERFORMANCE", "Your recorded " + skill.name
                    + " average is " + Math.round(skill.performance.scorePercent()) + "%."));
        }
        if (skill.requiredByRole) {
            reasons.add(new AdaptiveReasonDto("TARGET_ROLE_REQUIREMENT", skill.name
                    + " is listed as a required skill for your selected job description."));
        }
        if (skill.inPreparationPlan) {
            reasons.add(new AdaptiveReasonDto("PREPARATION_PLAN", skill.name
                    + " appears in your current preparation plan."));
        }
        if (skill.performance != null) {
            reasons.add(new AdaptiveReasonDto("DIFFICULTY_PROGRESSION", "Prior " + skill.name
                    + " performance sets this question to " + difficultyFor(skill.performance.scorePercent()) + "."));
        }
        return reasons;
    }

    private String chooseWeightedSkill(List<WeightedSkill> skills, int questionOrder) {
        Map<String, Integer> allocated = new HashMap<>();
        String selected = skills.get(0).name;
        for (int order = 1; order <= questionOrder; order++) {
            double bestPriority = -1;
            for (WeightedSkill skill : skills) {
                int previous = allocated.getOrDefault(normalize(skill.name), 0);
                double priority = skill.weight / (previous + 1.0);
                if (priority > bestPriority) {
                    bestPriority = priority;
                    selected = skill.name;
                }
            }
            allocated.merge(normalize(selected), 1, Integer::sum);
        }
        return selected;
    }

    private SkillGapAnalysis findRelevantSkillGap(InterviewSession session, Long userId) {
        Long resumeId = session.getResume() == null ? null : session.getResume().getId();
        Long jobDescriptionId = session.getJobDescription() == null ? null : session.getJobDescription().getId();
        if (resumeId != null && jobDescriptionId != null) {
            return skillGapRepository.findFirstByResumeIdAndJobDescriptionIdOrderByAnalyzedAtDesc(resumeId, jobDescriptionId)
                    .orElse(null);
        }
        if (resumeId != null) return skillGapRepository.findFirstByResumeIdOrderByAnalyzedAtDesc(resumeId).orElse(null);
        if (jobDescriptionId != null) return skillGapRepository.findFirstByJobDescriptionIdOrderByAnalyzedAtDesc(jobDescriptionId).orElse(null);
        return null;
    }

    private Map<String, String> readGapTypes(SkillGapAnalysis skillGap) {
        if (skillGap == null) return Map.of();
        Map<String, String> result = new HashMap<>();
        readSkillItems(skillGap.getPartialSkillsJson()).forEach(item -> result.put(normalize(item.skillName()), "WEAK"));
        readSkillItems(skillGap.getMissingSkillsJson()).forEach(item -> result.put(normalize(item.skillName()), "MISSING"));
        return result;
    }

    private List<SkillItem> readSkillItems(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception exception) { return List.of(); }
    }

    private Set<String> findCurrentPlanSkills(Long userId, Long skillGapId) {
        if (skillGapId == null) return Set.of();
        return preparationPlanRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(userId)
                .map(PreparationPlan::getPlanContentJson)
                .map(json -> readPlanSkills(json, skillGapId))
                .orElse(Set.of());
    }

    private Set<String> readPlanSkills(String json, Long skillGapId) {
        try {
            JsonNode content = objectMapper.readTree(json);
            if (content.path("skillGapAnalysisId").asLong(-1) != skillGapId) return Set.of();
            JsonNode items = content.path("skillAreas");
            Set<String> skills = new LinkedHashSet<>();
            if (items.isArray()) items.forEach(item -> {
                String skill = item.path("skillName").asText("");
                if (!skill.isBlank()) skills.add(skill);
            });
            return skills;
        } catch (Exception exception) {
            return Set.of();
        }
    }

    private void addSessionSkills(Map<String, SkillCandidate> candidates, InterviewSession session) {
        if (session.getResume() != null && session.getResume().getAnalysis() != null) {
            readStringList(session.getResume().getAnalysis().getExtractedSkillsJson())
                    .forEach(skill -> addCandidate(candidates, skill));
        }
        if (session.getJobDescription() != null && session.getJobDescription().getAnalysis() != null) {
            readStringList(session.getJobDescription().getAnalysis().getRequiredSkillsJson())
                    .forEach(skill -> addCandidate(candidates, skill));
        }
    }

    private void addCandidate(Map<String, SkillCandidate> candidates, String skill) {
        if (skill == null || skill.isBlank()) return;
        candidates.putIfAbsent(normalize(skill), new SkillCandidate(skill.trim()));
    }

    private boolean containsSkill(Collection<String> skills, String candidate) {
        String normalized = normalize(candidate);
        return skills.stream().anyMatch(skill -> normalize(skill).equals(normalized));
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception exception) { return List.of(); }
    }

    private String normalize(String skill) {
        return skill == null ? "" : skill.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+#.]+", " ")
                .trim().replaceAll("\\s+", " ");
    }

    private DifficultyLevel difficultyFor(double percent) {
        if (percent <= 40) return DifficultyLevel.EASY;
        if (percent <= 70) return DifficultyLevel.MEDIUM;
        return DifficultyLevel.HARD;
    }

    private double percent(double scoreOutOfTen) {
        return round(Math.max(0, Math.min(10, scoreOutOfTen)) * 10.0);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static final class SkillCandidate {
        private final String name;
        private SkillCandidate(String name) { this.name = name; }
    }

    private record WeightedSkill(String name, double weight, SkillPerformance performance,
                                 String gapType, boolean requiredByRole, boolean inPreparationPlan) {}

    private final class SkillAccumulator {
        private final String name;
        private final List<Double> scores = new ArrayList<>();
        private final List<Double> technical = new ArrayList<>();
        private final List<Double> completeness = new ArrayList<>();
        private final List<Double> clarity = new ArrayList<>();
        private final List<Double> communication = new ArrayList<>();
        private int mistakes;

        private SkillAccumulator(String name) { this.name = name; }

        private void add(AnswerEvaluation evaluation) {
            scores.add(evaluation.getOverallScore());
            add(technical, evaluation.getTechnicalAccuracyScore());
            add(completeness, evaluation.getCompletenessScore());
            add(clarity, evaluation.getClarityScore());
            Double communicationScore = communicationScore(evaluation.getDetailedFeedback());
            add(communication, communicationScore);
            if (evaluation.getOverallScore() < 5.0) mistakes++;
        }

        private void merge(SkillAccumulator source) {
            scores.addAll(source.scores);
            technical.addAll(source.technical);
            completeness.addAll(source.completeness);
            clarity.addAll(source.clarity);
            communication.addAll(source.communication);
            mistakes += source.mistakes;
        }

        private SkillPerformance toDto() {
            double average = average(scores);
            return new SkillPerformance(name, percent(average), percent(average(technical)),
                    percent(average(completeness)), percent(average(clarity)),
                    communication.isEmpty() ? null : percent(average(communication)),
                    average <= 4.0 ? "WEAK" : average <= 7.0 ? "MODERATE" : "STRONG", mistakes);
        }

        private void add(List<Double> values, Double value) {
            if (value != null) values.add(value);
        }

        private double average(List<Double> values) {
            return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        }
    }

    private Double communicationScore(String details) {
        if (details == null || details.isBlank()) return null;
        try {
            JsonNode value = objectMapper.readTree(details).path("communicationScore");
            return value.isNumber() ? value.asDouble() : null;
        } catch (Exception exception) {
            return null;
        }
    }
}