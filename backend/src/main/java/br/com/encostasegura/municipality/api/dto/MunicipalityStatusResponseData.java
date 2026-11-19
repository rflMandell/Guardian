package br.com.encostasegura.municipality.api.dto;

import java.time.Instant;
import java.util.UUID;

public record MunicipalityStatusResponseData(
        UUID municipalityId,
        String overallRiskLevel,
        int activeAlerts,
        String dataStatus,
        Instant lastUpdatedAt
) {}
