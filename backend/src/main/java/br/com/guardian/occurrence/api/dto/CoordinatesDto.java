package br.com.guardian.occurrence.api.dto;

import jakarta.validation.constraints.NotNull;

public record CoordinatesDto(
        @NotNull(message = "A latitude é obrigatória.")
        Double latitude,

        @NotNull(message = "A longitude é obrigatória.")
        Double longitude
) {}
