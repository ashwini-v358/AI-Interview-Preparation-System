package com.smartinterview.dto.interview;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO to submit an answer to the current question.
 */
@Data
public class SubmitAnswerRequest {

    @NotBlank(message = "Answer text is required")
    private String answerText;

    /** Time taken to answer in seconds (optional, client-measured) */
    private Integer timeTakenSeconds;
}
