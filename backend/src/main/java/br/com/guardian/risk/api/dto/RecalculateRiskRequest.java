package br.com.guardian.risk.api.dto;

import br.com.guardian.occurrence.domain.model.EventCategory;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RecalculateRiskRequest(
        @NotNull(message = "O identificador do município é obrigatório.")
        UUID municipalityId,

        UUID riskAreaId,

        @NotNull(message = "A categoria de evento é obrigatória.")
        EventCategory eventCategory
) {}
