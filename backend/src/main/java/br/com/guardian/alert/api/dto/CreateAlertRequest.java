package br.com.guardian.alert.api.dto;

import br.com.guardian.alert.domain.model.AlertOriginType;
import br.com.guardian.risk.domain.model.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateAlertRequest(
        @NotNull(message = "O identificador do município é obrigatório.")
        UUID municipalityId,

        @NotBlank(message = "O título do alerta é obrigatório.")
        @Size(max = 200, message = "O título não pode exceder 200 caracteres.")
        String title,

        @NotBlank(message = "A descrição/mensagem do alerta é obrigatória.")
        String description,

        @NotNull(message = "A severidade do alerta é obrigatória.")
        RiskLevel severity,

        AlertOriginType originType,

        Instant startsAt,

        Instant expiresAt,

        List<String> recommendations,

        UUID incidentId,

        UUID riskAssessmentId,

        String areaDescription
) {}
