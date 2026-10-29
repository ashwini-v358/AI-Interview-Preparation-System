package com.smartinterview.dto.interview;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full session state DTO — returned on start, next-question, and status calls.
 */
@Data
@Builder
public class InterviewSessionResponse {

    private Long sessionId;
    private String sessionTitle;
    private String targetRole;
    private String interviewType;
    private String status;
    private boolean adaptiveMode;
    private String currentDifficulty;
    private int targetQuestionCount;
    private int completedQuestionCount;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    // Current question (null if session completed)
    private QuestionDto currentQuestion;

    // Summary of all Q&A so far
    private List<QuestionAnswerDto> history;

    // IDs for linking
    private Long resumeId;
    private Long jobDescriptionId;
}
