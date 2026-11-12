package br.com.guardian.alert.api.dto;

import br.com.guardian.risk.domain.model.RiskLevel;

import java.time.Instant;
import java.util.List;

public record UpdateAlertRequest(
        RiskLevel severity,
        String description,
        Instant expiresAt,
        List<String> recommendations,
        String reason
) {}
