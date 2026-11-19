package br.com.encostasegura.safelocation.api.dto;

import br.com.encostasegura.safelocation.domain.model.SafeLocationOperationalStatus;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateSafeLocationAvailabilityRequest(
        @PositiveOrZero(message = "A ocupação deve ser maior ou igual a zero.")
        Integer occupancy,

        SafeLocationOperationalStatus operationalStatus
) {}
