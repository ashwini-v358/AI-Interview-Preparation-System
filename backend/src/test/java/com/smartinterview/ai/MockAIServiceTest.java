package com.smartinterview.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MockAIServiceTest {

    private final MockAIService service = new MockAIService(new ObjectMapper());

    @Test
    void generatesQuestionFromResumeProjectAndTargetRole() {
        String project = "Project: Smart Interview Platform built with Spring Boot and React.";
        InterviewContext context = new InterviewContext(
                "RESUME_BASED", "Java Backend Developer", List.of("Java", "Spring Boot"), "MEDIUM",
                List.of(), List.of(), List.of(), "Backend engineer", project,
                List.of(project), 4.0, "Backend Engineer", "Build Java APIs", List.of("Spring Boot"), 1, 5);

        InterviewQuestionResult result = service.generateInterviewQuestionResult(context);

        assertTrue(result.questionText().contains(project));
        assertTrue(result.questionText().contains("Java Backend Developer"));
        assertEquals(List.of(project), result.groundingEvidence());
        assertFalse(result.expectedKeyPoints().isEmpty());
    }

        @Test
        void prioritizesExplicitAdaptiveFocusOverJobSkillRoundRobin() {
                InterviewContext context = new InterviewContext(
                                "TECHNICAL", "Backend Engineer", List.of("Java", "SQL"), "EASY",
                                List.of(), List.of(), List.of(), null, null, List.of(), null, "Backend Engineer",
                                "SQL and Java role", List.of("Java", "Spring Boot"), 1, 5, "SQL");

                InterviewQuestionResult result = service.generateInterviewQuestionResult(context);

                assertEquals("SQL", result.targetSkill());
                assertTrue(result.questionText().contains("SQL"));
        }

    @Test
    void asksFollowUpUsingThePreviousBriefAnswer() {
        String answer = "I used Spring Boot and React.";
        AnswerEvaluationResult previous = new AnswerEvaluationResult(
                4, 4, 2, 6, 5, 3, 4, true, List.of("Named the tools"),
                List.of("Explain the backend responsibilities"), "Brief answer", "Add implementation details",
                true, "Spring Boot responsibilities", "", "{}");
        InterviewContext context = new InterviewContext(
                "COMPREHENSIVE_ADAPTIVE", "Backend Developer", List.of("Spring Boot"), "MEDIUM",
                List.of("Explain your project"), List.of(answer), List.of(previous), null, null,
                List.of(), null, null, null, List.of(), 2, 5);

        InterviewQuestionResult result = service.generateInterviewQuestionResult(context);

        assertTrue(result.followUp());
        assertTrue(result.questionText().contains(answer));
        assertTrue(result.groundingEvidence().contains(answer));
    }

    @Test
    void evaluatesAgainstTheQuestionKeyPoints() {
        InterviewContext context = new InterviewContext(
                "TECHNICAL", "Java Backend Developer", List.of("Spring Boot"), "MEDIUM",
                List.of(), List.of(), List.of(), null, null, List.of(), null, null, null,
                List.of(), 1, 3);

        AnswerEvaluationResult result = service.evaluateAnswer(
                "How did you secure the REST API?",
                "Spring Boot exposes the REST endpoints.",
                context,
                List.of("Spring Boot exposes REST endpoints", "JWT protects API requests"));

        assertTrue(result.relevanceScore() >= 0 && result.relevanceScore() <= 10);
        assertTrue(result.accuracyScore() >= 0 && result.accuracyScore() <= 10);
        assertEquals(List.of("JWT protects API requests"), result.missingPoints());
        assertTrue(result.followUpNeeded());
        assertNotNull(result.rawAiResponse());
    }

        @Test
        void finalReportAggregatesActualAnswerEvaluations() {
                AnswerEvaluationResult strong = new AnswerEvaluationResult(
                                8, 8, 7, 8, 7, 8, 8, true, List.of("Explained REST flow"),
                                List.of("Mention authentication"), "Good API explanation", "Describe authorization",
                                true, "Authentication", "", "{}");
                AnswerEvaluationResult weak = new AnswerEvaluationResult(
                                4, 4, 3, 5, 4, 3, 4, true, List.of("Named a technology"),
                                List.of("Explain database indexes"), "Needs more depth", "Add an index example",
                                true, "Database indexes", "", "{}");
                InterviewReportContext context = new InterviewReportContext("Java Backend Developer", "TECHNICAL",
                                "Built a Spring Boot service", "Java backend role", List.of(
                                new InterviewAnswerContext(1, "Explain the project", "Spring Boot REST API", List.of("REST"), strong),
                                new InterviewAnswerContext(2, "How do indexes help?", "They help", List.of("B-tree", "query"), weak)));

                InterviewReportResult report = service.generateInterviewReport(context);

                assertTrue(report.overallScore() > 0 && report.overallScore() <= 100);
                assertTrue(report.strugglingQuestionOrders().contains(2));
                assertTrue(report.areasToImprove().contains("Explain database indexes"));
                assertFalse(report.strengths().isEmpty());
                assertTrue(report.executiveSummary().contains("2 evaluated answers"));
        }
}
