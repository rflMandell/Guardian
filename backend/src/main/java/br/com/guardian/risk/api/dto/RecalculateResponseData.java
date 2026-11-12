package br.com.guardian.risk.api.dto;

import java.util.UUID;

public record RecalculateResponseData(
        String status,
        String message,
        UUID assessmentId
) {}
