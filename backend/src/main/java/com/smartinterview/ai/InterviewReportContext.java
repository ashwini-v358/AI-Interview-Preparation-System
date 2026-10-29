package com.smartinterview.ai;

import java.util.List;

public record InterviewReportContext(
        String targetRole,
        String interviewType,
        String resumeText,
        String jobDescriptionText,
        List<InterviewAnswerContext> answers
) {}
