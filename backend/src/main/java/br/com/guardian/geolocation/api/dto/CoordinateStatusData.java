package br.com.guardian.geolocation.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CoordinateStatusData(
        MunicipalitySummary municipality,
        String riskLevel,
        String dataStatus,
        int activeAlerts,
        List<String> riskFactors,
        Instant evaluatedAt
) {
    public record MunicipalitySummary(UUID id, String name) {}
}
