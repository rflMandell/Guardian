package br.com.guardian.occurrence.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LinkOccurrenceRequest(
        @NotNull(message = "O identificador do incidente é obrigatório.")
        UUID incidentId
) {}
