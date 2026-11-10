package br.com.guardian.shared.exception;

import java.util.List;

public class ValidationException extends RuntimeException {

    private final List<FieldErrorDetail> details;

    public ValidationException(String message, List<FieldErrorDetail> details) {
        super(message);
        this.details = details;
    }

    public ValidationException(String message) {
        super(message);
        this.details = List.of();
    }

    public List<FieldErrorDetail> getDetails() {
        return details;
    }
}
