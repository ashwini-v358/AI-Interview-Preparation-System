package com.smartinterview.exception;

/**
 * Thrown when a duplicate resource creation is attempted (e.g., duplicate email on register).
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
