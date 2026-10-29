package com.smartinterview.dto.interview;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO representing a single interview question.
 */
@Data
@Builder
public class QuestionDto {

    private Long questionId;
    private int questionOrder;
    private String questionText;
    private String category;
    private String targetSkill;
    private String difficultyLevel;
    private List<AdaptiveReasonDto> selectionReasons;
}
