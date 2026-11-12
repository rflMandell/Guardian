package br.com.guardian.risk.api.dto;

import br.com.guardian.occurrence.domain.model.EventCategory;
import br.com.guardian.risk.domain.model.ConfidenceLevel;
import br.com.guardian.risk.domain.model.RiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RiskAssessmentResponseData(
        UUID id,
        UUID municipalityId,
        UUID riskAreaId,
        RiskLevel riskLevel,
        EventCategory eventCategory,
        ConfidenceLevel confidence,
        BigDecimal probability,
        String dataStatus,
        List<RiskFactorData> factors,
        Instant evaluatedAt,
        Instant validUntil
) {}
