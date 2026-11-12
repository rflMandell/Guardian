package br.com.guardian.risk.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RiskFactorData(
        UUID id,
        String factorType,
        String name,
        String contribution,
        String description,
        BigDecimal weight
) {}
