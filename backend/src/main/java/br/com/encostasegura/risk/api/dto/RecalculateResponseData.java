package br.com.encostasegura.risk.api.dto;

import java.util.UUID;

public record RecalculateResponseData(
        String status,
        String message,
        UUID assessmentId
) {}
