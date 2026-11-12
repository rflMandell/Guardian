package br.com.guardian.safelocation.api.dto;

import br.com.guardian.safelocation.domain.model.SafeLocationOperationalStatus;
import br.com.guardian.safelocation.domain.model.SafeLocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateSafeLocationRequest(
        @NotNull(message = "O identificador do município é obrigatório.")
        UUID municipalityId,

        @NotBlank(message = "O nome do local é obrigatório.")
        @Size(max = 150, message = "O nome não pode exceder 150 caracteres.")
        String name,

        String description,

        SafeLocationType type,

        @Size(max = 255, message = "O endereço não pode exceder 255 caracteres.")
        String address,

        @NotNull(message = "A latitude é obrigatória.")
        Double latitude,

        @NotNull(message = "A longitude é obrigatória.")
        Double longitude,

        @PositiveOrZero(message = "A capacidade deve ser maior ou igual a zero.")
        Integer capacity,

        Boolean accessibility,

        SafeLocationOperationalStatus operationalStatus
) {}
