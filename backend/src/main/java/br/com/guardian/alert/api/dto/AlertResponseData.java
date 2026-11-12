package br.com.guardian.alert.api.dto;

import br.com.guardian.alert.domain.model.AlertOriginType;
import br.com.guardian.alert.domain.model.AlertStatus;
import br.com.guardian.risk.domain.model.RiskLevel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AlertResponseData(
        UUID id,
        UUID municipalityId,
        String title,
        String description,
        RiskLevel severity,
        AlertOriginType originType,
        AlertStatus status,
        Instant issuedAt,
        Instant expiresAt,
        String dataStatus,
        List<AlertAreaDto> areas,
        List<String> recommendations
) {}
