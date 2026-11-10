package br.com.guardian.municipality.api.dto;

import java.time.Instant;
import java.util.UUID;

public record MunicipalityMemberData(
        UUID id,
        UUID userId,
        String userName,
        String userEmail,
        UUID municipalityId,
        String status,
        Instant createdAt
) {}
