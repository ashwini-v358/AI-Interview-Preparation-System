package com.smartinterview.interview;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinterview.ai.AIService;
import com.smartinterview.ai.AnswerEvaluationResult;
import com.smartinterview.ai.InterviewContext;
import com.smartinterview.ai.InterviewQuestionResult;
import com.smartinterview.dto.interview.*;
import com.smartinterview.entity.*;
import com.smartinterview.entity.enums.DifficultyLevel;
import com.smartinterview.entity.enums.InterviewStatus;
import com.smartinterview.entity.enums.InterviewType;
import com.smartinterview.exception.ResourceNotFoundException;
import com.smartinterview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * InterviewService — orchestrates the full adaptive interview simulation lifecycle:
 *
 *   1. Start session: create DB record, generate first question
 *   2. Submit answer: persist answer, evaluate with AI, generate next question
 *   3. Get state: return current question + Q&A history
 *   4. Complete session: mark completed, compute summary scores
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final CandidateAnswerRepository answerRepository;
    private final AnswerEvaluationRepository evaluationRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jdRepository;
    private final InterviewEvolutionService evolutionService;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.max-context-chars:24000}")
    private int maxAiContextChars;


    // ─── Start Session ────────────────────────────────────────────────────────

    @Transactional
    public InterviewSessionResponse startSession(StartInterviewRequest request, String userEmail) {
        return startSession(request, userEmail, false);
    }

    @Transactional
    public InterviewSessionResponse startAdaptiveSession(StartInterviewRequest request, String userEmail) {
        return startSession(request, userEmail, true);
    }

    @Transactional(readOnly = true)
    public AdaptiveProfileResponse getAdaptiveProfile(String userEmail) {
        return evolutionService.getProfile(userEmail);
    }

    private InterviewSessionResponse startSession(StartInterviewRequest request, String userEmail,
                                                  boolean adaptiveMode) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Resume resume = null;
        if (request.getResumeId() != null) {
            resume = resumeRepository.findByIdAndUserId(request.getResumeId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resume", request.getResumeId()));
        }

        JobDescription jd = null;
        if (request.getJobDescriptionId() != null) {
            jd = jdRepository.findByIdAndUserId(request.getJobDescriptionId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("JobDescription", request.getJobDescriptionId()));
        }

        InterviewType type = parseInterviewType(request.getInterviewType());
        DifficultyLevel difficulty = parseDifficulty(request.getDifficulty());

        InterviewSession session = InterviewSession.builder()
                .user(user)
                .resume(resume)
                .jobDescription(jd)
                .sessionTitle(request.getSessionTitle())
                .targetRole(resolveTargetRole(request.getTargetRole(), jd))
                .interviewType(type)
                .adaptiveMode(adaptiveMode)
                .status(InterviewStatus.IN_PROGRESS)
                .currentDifficulty(difficulty)
                .targetQuestionCount(request.getTargetQuestionCount())
                .completedQuestionCount(0)
                .startedAt(LocalDateTime.now())
                .build();
        session = sessionRepository.save(session);
        log.info("Interview session started: id={}, user={}", session.getId(), userEmail);

        // Generate first question
        InterviewQuestion q1 = generateAndPersistQuestion(session, List.of(), List.of(), List.of(), 1);

        return buildSessionResponse(session, q1, List.of());
    }

    // ─── Get Session State ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public InterviewSessionResponse getSession(Long sessionId, String userEmail) {
        InterviewSession session = loadSessionForUser(sessionId, userEmail);
        List<InterviewQuestion> questions = questionRepository.findByInterviewSessionIdOrderByQuestionOrderAsc(session.getId());

        // The current question is the last one without an answer
        InterviewQuestion current = questions.stream()
                .filter(q -> q.getCandidateAnswer() == null)
                .findFirst()
                .orElse(null);

        List<QuestionAnswerDto> history = questions.stream()
                .filter(q -> q.getCandidateAnswer() != null)
                .map(this::toQADto)
                .collect(Collectors.toList());

        return buildSessionResponse(session, current, history);
    }

    // ─── List Sessions ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<InterviewSessionResponse> getMySessions(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(s -> buildSessionResponse(s, null, List.of()))
                .collect(Collectors.toList());
    }

    // ─── Submit Answer ────────────────────────────────────────────────────────

    @Transactional
    public InterviewSessionResponse submitAnswer(Long sessionId, Long questionId, SubmitAnswerRequest request, String userEmail) {
        InterviewSession session = loadSessionForUser(sessionId, userEmail);

        if (session.getStatus() == InterviewStatus.COMPLETED) {
            throw new IllegalStateException("Session is already completed");
        }

        InterviewQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", questionId));

        if (!question.getInterviewSession().getId().equals(sessionId)) {
            throw new AccessDeniedException("Question does not belong to this session");
        }

        if (question.getCandidateAnswer() != null) {
            throw new IllegalStateException("This question has already been answered");
        }

        // 1. Persist answer
        CandidateAnswer answer = CandidateAnswer.builder()
                .interviewQuestion(question)
                .answerText(request.getAnswerText())
                .timeTakenSeconds(request.getTimeTakenSeconds())
                .submittedAt(LocalDateTime.now())
                .build();
            question.setCandidateAnswer(answer);
        answer = answerRepository.save(answer);

        // 2. Evaluate answer with AI
            List<InterviewQuestion> answeredQuestions = questionRepository
                .findByInterviewSessionIdOrderByQuestionOrderAsc(session.getId()).stream()
                .filter(item -> item.getCandidateAnswer() != null)
                .toList();
            List<String> priorQuestionTexts = answeredQuestions.stream()
                .map(InterviewQuestion::getQuestionText).toList();
            List<String> priorAnswers = answeredQuestions.stream()
                .map(item -> item.getCandidateAnswer().getAnswerText()).toList();
            List<AnswerEvaluationResult> priorEvaluations = answeredQuestions.stream()
                .map(InterviewQuestion::getCandidateAnswer)
                .map(CandidateAnswer::getEvaluation)
                .filter(java.util.Objects::nonNull)
                .map(this::toEvaluationResult)
                .toList();
            InterviewContext context = buildContext(session, priorQuestionTexts, priorAnswers,
                priorEvaluations, question.getQuestionOrder());
            List<String> expectedKeyPoints = fromJson(question.getExpectedKeyPointsJson());
        AnswerEvaluationResult evalResult = aiService.evaluateAnswer(
                question.getQuestionText(), request.getAnswerText(), context, expectedKeyPoints);

        AnswerEvaluation evaluation = AnswerEvaluation.builder()
                .candidateAnswer(answer)
                .technicalAccuracyScore(evalResult.accuracyScore())
                .completenessScore(evalResult.completenessScore())
                .clarityScore(evalResult.clarityScore())
                .overallScore(evalResult.overallScore())
                .strengths(String.join("\n", evalResult.strengths()))
                .areasOfImprovement(String.join("\n", evalResult.missingPoints()))
                .detailedFeedback(toJson(toStoredEvaluation(evalResult)))
                .followUpTopic(evalResult.followUpTopic())
                .evaluatedAt(LocalDateTime.now())
                .build();
            answer.setEvaluation(evaluation);
        evaluationRepository.save(evaluation);

        // 3. Update session progress
        session.setCompletedQuestionCount(session.getCompletedQuestionCount() + 1);

        // 4. Check if we should generate next question or complete
        int nextOrder = session.getCompletedQuestionCount() + 1;
        InterviewQuestion nextQuestion = null;

        if (session.getCompletedQuestionCount() < session.getTargetQuestionCount()) {
            // Adapt difficulty based on score
            adaptDifficulty(session, evalResult.overallScore());

            // Gather history for context
            List<InterviewQuestion> prevQuestions = questionRepository
                    .findByInterviewSessionIdOrderByQuestionOrderAsc(session.getId());
                List<InterviewQuestion> answered = prevQuestions.stream()
                    .filter(q -> q.getCandidateAnswer() != null)
                    .toList();
                List<String> prevQTexts = answered.stream().map(InterviewQuestion::getQuestionText).toList();
                List<String> prevATexts = answered.stream()
                    .map(q -> q.getCandidateAnswer().getAnswerText()).toList();
                List<AnswerEvaluationResult> evaluations = answered.stream()
                    .map(InterviewQuestion::getCandidateAnswer)
                    .map(CandidateAnswer::getEvaluation)
                    .filter(java.util.Objects::nonNull)
                    .map(this::toEvaluationResult)
                    .toList();

                nextQuestion = generateAndPersistQuestion(session, prevQTexts, prevATexts, evaluations, nextOrder);
        } else {
            // Session complete
            session.setStatus(InterviewStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            log.info("Interview session {} completed", session.getId());
        }

        sessionRepository.save(session);

        // Build history
        List<InterviewQuestion> allQuestions = questionRepository.findByInterviewSessionIdOrderByQuestionOrderAsc(session.getId());
        List<QuestionAnswerDto> history = allQuestions.stream()
                .filter(q -> q.getCandidateAnswer() != null)
                .map(this::toQADto)
                .collect(Collectors.toList());

        return buildSessionResponse(session, nextQuestion, history);
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private InterviewQuestion generateAndPersistQuestion(InterviewSession session,
                                  List<String> prevQuestions,
                                  List<String> prevAnswers,
                                  List<AnswerEvaluationResult> previousEvaluations,
                                  int order) {
        InterviewContext context = buildContext(session, prevQuestions, prevAnswers,
            previousEvaluations, order);
        AdaptiveQuestionPlan adaptivePlan = session.isAdaptiveMode()
            ? evolutionService.selectQuestion(session, order) : null;
        if (adaptivePlan != null) {
            session.setCurrentDifficulty(adaptivePlan.difficulty());
            context = new InterviewContext(context.interviewType(), context.targetRole(), context.targetSkills(),
                adaptivePlan.difficulty().name(), context.previousQuestions(), context.previousAnswers(),
                context.previousEvaluations(), context.resumeSummary(), context.resumeText(),
                context.projectEvidence(), context.experienceYears(), context.jobDescriptionTitle(),
                context.jobDescriptionText(), context.jobRequiredSkills(), context.questionNumber(),
                context.totalQuestions(), adaptivePlan.focusSkill(), context.skillGapSignals(),
                context.performanceSignals(), context.preparationSignals(),
                adaptivePlan.reasons().stream().map(AdaptiveReasonDto::description).toList());
        }
        InterviewQuestionResult generated = aiService.generateInterviewQuestionResult(context);
        if (adaptivePlan != null && adaptivePlan.focusSkill() != null
            && (generated.targetSkill() == null
            || !generated.targetSkill().equalsIgnoreCase(adaptivePlan.focusSkill()))) {
            String focus = adaptivePlan.focusSkill();
            generated = new InterviewQuestionResult(
                "For " + focus + ", explain a practical implementation, the decisions you would make, "
                    + "and how you would verify the result.",
                "Adaptive focus", focus,
                List.of("Explain the core concept", "Describe an implementation", "Discuss a trade-off"),
                List.of(focus), false, generated.questionType(), generated.difficulty(), generated.reason());
        }

        InterviewQuestion question = InterviewQuestion.builder()
                .interviewSession(session)
                .questionOrder(order)
            .questionText(generated.questionText())
                .difficultyLevel(session.getCurrentDifficulty())
                .category(resolveCategory(generated))
                    .targetSkill(adaptivePlan != null && adaptivePlan.focusSkill() != null
                        ? adaptivePlan.focusSkill() : generated.targetSkill())
            .expectedKeyPointsJson(toJson(generated.expectedKeyPoints()))
                    .selectionReasonsJson(toJson(buildSelectionReasons(adaptivePlan, generated)))
                .createdAt(LocalDateTime.now())
                .build();

        return questionRepository.save(question);
    }

    /**
     * Prefers the provider's question type for display, falling back to its category label.
     * The existing {@code category} column and API field are reused so no schema or API
     * contract change is needed.
     */
    private String resolveCategory(InterviewQuestionResult generated) {
        if (generated.questionType() != null && !generated.questionType().isBlank()) {
            return generated.questionType();
        }
        return generated.category();
    }

    /**
     * Combines the platform's own adaptive reasons with the provider's "why this question"
     * rationale, so the candidate sees one consistent explanation. The rationale is omitted
     * when the provider supplies none, which keeps deterministic mock runs unchanged.
     */
    private List<AdaptiveReasonDto> buildSelectionReasons(AdaptiveQuestionPlan adaptivePlan,
                                                          InterviewQuestionResult generated) {
        List<AdaptiveReasonDto> reasons = new ArrayList<>(
            adaptivePlan == null ? List.of() : adaptivePlan.reasons());
        if (generated.reason() != null && !generated.reason().isBlank()) {
            reasons.add(new AdaptiveReasonDto("AI_RATIONALE", generated.reason().trim()));
        }
        return reasons;
    }


    private void adaptDifficulty(InterviewSession session, double score) {
        if (score >= 8.0 && session.getCurrentDifficulty() != DifficultyLevel.HARD) {
            session.setCurrentDifficulty(DifficultyLevel.HARD);
        } else if (score < 5.0 && session.getCurrentDifficulty() != DifficultyLevel.EASY) {
            session.setCurrentDifficulty(DifficultyLevel.EASY);
        }
    }

    private List<String> getTargetSkills(InterviewSession session) {
        List<String> skills = new ArrayList<>();
        if (session.getResume() != null && session.getResume().getAnalysis() != null) {
            skills.addAll(fromJson(session.getResume().getAnalysis().getExtractedSkillsJson()));
        }
        if (session.getJobDescription() != null && session.getJobDescription().getAnalysis() != null) {
            skills.addAll(fromJson(session.getJobDescription().getAnalysis().getRequiredSkillsJson()));
        }
        return skills.stream().filter(skill -> skill != null && !skill.isBlank()).distinct().limit(20).toList();
    }

    private String getResumeSummary(InterviewSession session) {
        if (session.getResume() != null && session.getResume().getAnalysis() != null) {
            return session.getResume().getAnalysis().getSummary();
        }
        return null;
    }

    private InterviewContext buildContext(InterviewSession session,
                                          List<String> previousQuestions,
                                          List<String> previousAnswers,
                                          List<AnswerEvaluationResult> previousEvaluations,
                                          int questionNumber) {
        Resume resume = session.getResume();
        ResumeAnalysis resumeAnalysis = resume != null ? resume.getAnalysis() : null;
        JobDescription jd = session.getJobDescription();
        JobDescriptionAnalysis jdAnalysis = jd != null ? jd.getAnalysis() : null;
        String resumeText = resume != null ? limitText(resume.getRawText(), maxAiContextChars / 2) : null;
        List<String> boundedQuestions = limitHistory(previousQuestions, 400);
        List<String> boundedAnswers = limitHistory(previousAnswers, 900);
        List<AnswerEvaluationResult> boundedEvaluations = previousEvaluations == null
            ? List.of()
            : previousEvaluations.stream().skip(Math.max(0, previousEvaluations.size() - 5)).toList();
        InterviewContextSignals signals = loadSignals(session);
        return new InterviewContext(
                session.getInterviewType().name(),
                resolveTargetRole(session.getTargetRole(), jd),
                getTargetSkills(session),
                session.getCurrentDifficulty().name(),
                boundedQuestions,
                boundedAnswers,
                boundedEvaluations,
                limitText(getResumeSummary(session), 1200),
                resumeText,
                extractProjectEvidence(resumeText),
                resumeAnalysis != null ? resumeAnalysis.getExperienceYears() : null,
                jd != null ? jd.getTitle() : null,
                jd != null ? limitText(jd.getRawText(), maxAiContextChars / 4) : null,
                jdAnalysis != null ? fromJson(jdAnalysis.getRequiredSkillsJson()) : List.of(),
                questionNumber,
                session.getTargetQuestionCount(),
                null,
                signals.skillGapSignals(),
                signals.performanceSignals(),
                signals.preparationSignals(),
                List.of()
        );
    }

    /**
     * Loads skill gaps, previous performance and preparation coverage for the session owner.
     * Reading is delegated to {@link InterviewEvolutionService} so question generation and
     * adaptive skill selection always agree on the candidate's evidence, and so the lookups
     * stay scoped to the authenticated user's own records.
     */
    private InterviewContextSignals loadSignals(InterviewSession session) {
        try {
            InterviewContextSignals signals = evolutionService.loadContextSignals(session);
            return signals == null ? InterviewContextSignals.empty() : signals;
        } catch (RuntimeException ex) {
            log.warn("Could not load interview context signals for session {}: {}",
                    session.getId(), ex.getMessage());
            return InterviewContextSignals.empty();
        }
    }

    private String resolveTargetRole(String requestedRole, JobDescription jd) {
        if (requestedRole != null && !requestedRole.isBlank()) return requestedRole.trim();
        if (jd == null) return null;
        if (jd.getAnalysis() != null && jd.getAnalysis().getRoleTitle() != null
                && !jd.getAnalysis().getRoleTitle().isBlank()) {
            return jd.getAnalysis().getRoleTitle();
        }
        return jd.getTitle();
    }

    private List<String> extractProjectEvidence(String resumeText) {
        if (resumeText == null || resumeText.isBlank()) return List.of();
        return resumeText.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> line.toLowerCase().matches(".*(project|built|developed|implemented|designed).*"))
                .map(line -> line.length() > 300 ? line.substring(0, 300) : line)
                .limit(12)
                .toList();
    }

    private List<String> limitHistory(List<String> values, int entryLimit) {
        if (values == null) return List.of();
        return values.stream()
                .skip(Math.max(0, values.size() - 5))
                .map(value -> limitText(value, entryLimit))
                .toList();
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, Math.max(0, maxLength));
    }

    private AnswerEvaluationResult toEvaluationResult(AnswerEvaluation evaluation) {
        try {
            StoredEvaluation stored = objectMapper.readValue(evaluation.getDetailedFeedback(), StoredEvaluation.class);
            return stored.toResult(evaluation);
        } catch (Exception ignored) {
            return new AnswerEvaluationResult(
                    score(evaluation.getOverallScore()), score(evaluation.getTechnicalAccuracyScore()),
                    score(evaluation.getCompletenessScore()), score(evaluation.getClarityScore()),
                    score(evaluation.getClarityScore()), score(evaluation.getTechnicalAccuracyScore()),
                    score(evaluation.getOverallScore()), true,
                    evaluation.getStrengths() == null ? List.of() : List.of(evaluation.getStrengths()),
                    evaluation.getAreasOfImprovement() == null ? List.of() : List.of(evaluation.getAreasOfImprovement()),
                    evaluation.getDetailedFeedback(), evaluation.getAreasOfImprovement(), false,
                    evaluation.getFollowUpTopic(), evaluation.getAreasOfImprovement(), null);
        }
    }

    private StoredEvaluation toStoredEvaluation(AnswerEvaluationResult result) {
        return new StoredEvaluation(result.relevanceScore(), result.accuracyScore(),
                result.completenessScore(), result.clarityScore(), result.communicationScore(),
                result.depthScore(), result.overallScore(), result.answeredQuestion(), result.strengths(),
                result.missingPoints(), result.feedback(), result.suggestedImprovement(),
                result.followUpNeeded(), result.followUpTopic(), result.modelAnswer());
    }

    private double score(Double value) {
        return value == null ? 0 : value;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialize interview data", ex);
        }
    }

    private InterviewSession loadSessionForUser(Long sessionId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession", sessionId));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not own this session");
        }
        return session;
    }

    private InterviewSessionResponse buildSessionResponse(InterviewSession session,
                                                           InterviewQuestion currentQuestion,
                                                           List<QuestionAnswerDto> history) {
        QuestionDto currentDto = currentQuestion == null ? null : QuestionDto.builder()
                .questionId(currentQuestion.getId())
                .questionOrder(currentQuestion.getQuestionOrder())
                .questionText(currentQuestion.getQuestionText())
                .category(currentQuestion.getCategory())
                .targetSkill(currentQuestion.getTargetSkill())
                .difficultyLevel(currentQuestion.getDifficultyLevel().name())
                .selectionReasons(readAdaptiveReasons(currentQuestion.getSelectionReasonsJson()))
                .build();

        return InterviewSessionResponse.builder()
                .sessionId(session.getId())
                .sessionTitle(session.getSessionTitle())
            .targetRole(session.getTargetRole())
                .interviewType(session.getInterviewType().name())
                .status(session.getStatus().name())
                .adaptiveMode(session.isAdaptiveMode())
                .currentDifficulty(session.getCurrentDifficulty().name())
                .targetQuestionCount(session.getTargetQuestionCount())
                .completedQuestionCount(session.getCompletedQuestionCount())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .createdAt(session.getCreatedAt())
                .currentQuestion(currentDto)
                .history(history)
                .resumeId(session.getResume() != null ? session.getResume().getId() : null)
                .jobDescriptionId(session.getJobDescription() != null ? session.getJobDescription().getId() : null)
                .build();
    }

    private QuestionAnswerDto toQADto(InterviewQuestion q) {
        CandidateAnswer ans = q.getCandidateAnswer();
        AnswerEvaluation eval = ans != null ? ans.getEvaluation() : null;
        AnswerEvaluationResult details = eval != null ? toEvaluationResult(eval) : null;

        return QuestionAnswerDto.builder()
                .questionId(q.getId())
                .questionOrder(q.getQuestionOrder())
                .questionText(q.getQuestionText())
                .difficultyLevel(q.getDifficultyLevel().name())
                .answerId(ans != null ? ans.getId() : null)
                .answerText(ans != null ? ans.getAnswerText() : null)
                .timeTakenSeconds(ans != null ? ans.getTimeTakenSeconds() : null)
                .evaluationId(eval != null ? eval.getId() : null)
                .technicalAccuracyScore(eval != null ? eval.getTechnicalAccuracyScore() : null)
                .completenessScore(eval != null ? eval.getCompletenessScore() : null)
                .clarityScore(eval != null ? eval.getClarityScore() : null)
                .overallScore(eval != null ? eval.getOverallScore() : null)
                .strengths(eval != null ? eval.getStrengths() : null)
                .areasOfImprovement(eval != null ? eval.getAreasOfImprovement() : null)
                .detailedFeedback(eval != null ? eval.getDetailedFeedback() : null)
                .relevanceScore(details != null ? details.relevanceScore() : null)
                .communicationScore(details != null ? details.communicationScore() : null)
                .depthScore(details != null ? details.depthScore() : null)
                .answeredQuestion(details != null ? details.answeredQuestion() : null)
                .followUpNeeded(details != null ? details.followUpNeeded() : null)
                .strengthsList(details != null ? details.strengths() : List.of())
                .missingPoints(details != null ? details.missingPoints() : List.of())
                .feedback(details != null ? details.feedback() : null)
                .suggestedImprovement(details != null ? details.suggestedImprovement() : null)
                .followUpTopic(details != null ? details.followUpTopic() : null)
                .selectionReasons(readAdaptiveReasons(q.getSelectionReasonsJson()))
                .build();
    }

    private List<AdaptiveReasonDto> readAdaptiveReasons(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception exception) { return List.of(); }
    }

    private InterviewType parseInterviewType(String val) {
        try { return InterviewType.valueOf(val); }
        catch (Exception e) { return InterviewType.COMPREHENSIVE_ADAPTIVE; }
    }

    private DifficultyLevel parseDifficulty(String val) {
        try { return DifficultyLevel.valueOf(val); }
        catch (Exception e) { return DifficultyLevel.MEDIUM; }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return new ArrayList<>(); }
    }

    private record StoredEvaluation(
            Double relevanceScore,
            Double accuracyScore,
            Double completenessScore,
            Double clarityScore,
            Double communicationScore,
            Double depthScore,
            Double overallScore,
            Boolean answeredQuestion,
            List<String> strengths,
            List<String> missingPoints,
            String feedback,
            String suggestedImprovement,
            Boolean followUpNeeded,
            String followUpTopic,
            String modelAnswer) {

        private AnswerEvaluationResult toResult(AnswerEvaluation evaluation) {
            return new AnswerEvaluationResult(value(relevanceScore), value(accuracyScore),
                    value(completenessScore), value(clarityScore), value(communicationScore),
                    value(depthScore), value(overallScore), Boolean.TRUE.equals(answeredQuestion),
                    strengths == null ? List.of() : strengths,
                    missingPoints == null ? List.of() : missingPoints,
                    feedback, suggestedImprovement, Boolean.TRUE.equals(followUpNeeded),
                    followUpTopic, modelAnswer, null);
        }

        private double value(Double score) {
            return score == null ? 0 : score;
        }
    }
}
