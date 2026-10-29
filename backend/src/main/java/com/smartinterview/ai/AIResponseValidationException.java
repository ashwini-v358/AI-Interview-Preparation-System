package com.smartinterview.ai;

public class AIResponseValidationException extends RuntimeException {
    public AIResponseValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AIResponseValidationException(String message) {
        super(message);
    }
}
