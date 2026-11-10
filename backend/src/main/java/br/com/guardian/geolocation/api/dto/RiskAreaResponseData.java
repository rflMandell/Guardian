package br.com.guardian.geolocation.api.dto;

import java.time.Instant;
import java.util.UUID;

public record RiskAreaResponseData(
        UUID id,
        UUID municipalityId,
        UUID monitoredAreaId,
        String eventType,
        String vulnerabilityLevel,
        boolean active,
        Instant createdAt
) {}
