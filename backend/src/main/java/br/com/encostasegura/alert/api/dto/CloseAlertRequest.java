package br.com.encostasegura.alert.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CloseAlertRequest(
        @NotBlank(message = "A justificativa de encerramento é obrigatória.")
        String reason
) {}
