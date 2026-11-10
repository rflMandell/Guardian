package br.com.guardian.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldErrorDetail> details
) {
    public ErrorResponse(int status, String code, String message, String path) {
        this(Instant.now(), status, code, message, path, null);
    }

    public ErrorResponse(int status, String code, String message, String path, List<FieldErrorDetail> details) {
        this(Instant.now(), status, code, message, path, details);
    }
}
