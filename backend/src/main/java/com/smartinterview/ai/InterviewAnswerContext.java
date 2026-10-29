package com.smartinterview.ai;

import java.util.List;

public record InterviewAnswerContext(
        int questionOrder,
        String questionText,
        String answerText,
        List<String> expectedKeyPoints,
        AnswerEvaluationResult evaluation
) {}
