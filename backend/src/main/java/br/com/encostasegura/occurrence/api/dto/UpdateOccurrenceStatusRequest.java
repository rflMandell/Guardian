package br.com.encostasegura.occurrence.api.dto;

import br.com.encostasegura.occurrence.domain.model.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateOccurrenceStatusRequest(
        @NotNull(message = "O novo status é obrigatório.")
        ReportStatus status,

        @Size(max = 1000, message = "A descrição/justificativa não pode exceder 1000 caracteres.")
        String description
) {}
