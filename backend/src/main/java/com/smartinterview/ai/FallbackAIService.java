package com.smartinterview.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * FallbackAIService — the {@link AIService} actually injected when
 * {@code app.ai.provider=gemini}.
 *
 * Question generation therefore flows:
 *
 *   InterviewService → AIService → FallbackAIService → GeminiAIService → Gemini API
 *                                                        ↓ on any failure
 *                                                    MockAIService
 *
 * Behaviour:
 * - Every call is attempted on the primary provider first.
 * - Any failure (missing key, timeout, HTTP error, rate limit, malformed or unusable JSON)
 *   is logged as a warning — never including the API key — and retried on {@link MockAIService},
 *   so an existing interview always continues and the candidate never receives a 5xx.
 * - The mock fallback is deterministic, which keeps the test suite reproducible.
 * - When {@code app.ai.fallback-enabled=false} the failure is propagated instead, so a
 *   misconfigured deployment fails loudly rather than silently degrading.
 *
 * Fallback count is exposed through {@link #fallbackCount()} for observability and tests.
 */
@Slf4j
@Primary
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "gemini")
public class FallbackAIService implements AIService {

    private final AIService primary;
    private final AIService fallback;
    private final boolean fallbackEnabled;
    private final AtomicLong fallbackCount = new AtomicLong();

    public FallbackAIService(
            GeminiAIService primary,
            MockAIService fallback,
            @Value("${app.ai.fallback-enabled:true}") boolean fallbackEnabled) {
        this.primary = primary;
        this.fallback = fallback;
        this.fallbackEnabled = fallbackEnabled;
    }

    /** Number of calls that were served by the fallback provider. */
    public long fallbackCount() {
        return fallbackCount.get();
    }

    @Override
    public ResumeAnalysisResult analyzeResume(String rawResumeText) {
        return run("analyzeResume",
                () -> primary.analyzeResume(rawResumeText),
                () -> fallback.analyzeResume(rawResumeText));
    }

    @Override
    public JobDescriptionAnalysisResult analyzeJobDescription(String rawJdText) {
        return run("analyzeJobDescription",
                () -> primary.analyzeJobDescription(rawJdText),
                () -> fallback.analyzeJobDescription(rawJdText));
    }

    @Override
    public String generateInterviewQuestion(InterviewContext context) {
        return generateInterviewQuestionResult(context).questionText();
    }

    @Override
    public InterviewQuestionResult generateInterviewQuestionResult(InterviewContext context) {
        return run("generateInterviewQuestion",
                () -> primary.generateInterviewQuestionResult(context),
                () -> fallback.generateInterviewQuestionResult(context));
    }

    @Override
    public AnswerEvaluationResult evaluateAnswer(String question, String answer, InterviewContext context) {
        return run("evaluateAnswer",
                () -> primary.evaluateAnswer(question, answer, context),
                () -> fallback.evaluateAnswer(question, answer, context));
    }

    @Override
    public AnswerEvaluationResult evaluateAnswer(
            String question,
            String answer,
            InterviewContext context,
            List<String> expectedKeyPoints) {
        return run("evaluateAnswer",
                () -> primary.evaluateAnswer(question, answer, context, expectedKeyPoints),
                () -> fallback.evaluateAnswer(question, answer, context, expectedKeyPoints));
    }

    @Override
    public InterviewReportResult generateInterviewReport(InterviewReportContext context) {
        return run("generateInterviewReport",
                () -> primary.generateInterviewReport(context),
                () -> fallback.generateInterviewReport(context));
    }

    @Override
    public ReadinessStrategyResult generateReadinessStrategy(ReadinessStrategyContext context) {
        return run("generateReadinessStrategy",
                () -> primary.generateReadinessStrategy(context),
                () -> fallback.generateReadinessStrategy(context));
    }

    private <T> T run(String operation, SupplierCall<T> primaryCall, SupplierCall<T> fallbackCall) {
        try {
            return primaryCall.call();
        } catch (RuntimeException primaryFailure) {
            if (!fallbackEnabled) {
                throw primaryFailure;
            }
            fallbackCount.incrementAndGet();
            log.warn("AI provider '{}' failed during {} ({}); serving the deterministic mock response instead",
                    "gemini", operation, describe(primaryFailure));
            return fallbackCall.call();
        }
    }

    private String describe(RuntimeException failure) {
        if (failure instanceof AIProviderException || failure instanceof AIResponseValidationException) {
            return failure.getMessage();
        }
        return failure.getClass().getSimpleName();
    }

    @FunctionalInterface
    private interface SupplierCall<T> {
        T call();
    }
}
