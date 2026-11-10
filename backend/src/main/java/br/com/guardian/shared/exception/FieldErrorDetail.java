package br.com.guardian.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldErrorDetail(
        String field,
        String message
) {}
