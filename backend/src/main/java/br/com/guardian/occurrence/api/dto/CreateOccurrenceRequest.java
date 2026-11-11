package br.com.guardian.occurrence.api.dto;

import br.com.guardian.occurrence.domain.model.EventCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateOccurrenceRequest(
        @NotNull(message = "A categoria do evento é obrigatória.")
        EventCategory eventCategory,

        @Size(max = 2000, message = "A descrição não pode exceder 2000 caracteres.")
        String description,

        @NotNull(message = "A localização geográfica é obrigatória.")
        @Valid
        CoordinatesDto location,

        Instant observedAt,

        Boolean anonymous
) {}
