package br.com.guardian.safelocation.api.dto;

import br.com.guardian.occurrence.api.dto.CoordinatesDto;
import br.com.guardian.safelocation.domain.model.SafeLocationAvailability;
import br.com.guardian.safelocation.domain.model.SafeLocationOperationalStatus;
import br.com.guardian.safelocation.domain.model.SafeLocationType;

import java.time.Instant;
import java.util.UUID;

public record SafeLocationResponseData(
        UUID id,
        UUID municipalityId,
        String name,
        String description,
        SafeLocationType type,
        String address,
        CoordinatesDto location,
        Integer capacity,
        Integer occupancy,
        SafeLocationAvailability availability,
        boolean accessibility,
        SafeLocationOperationalStatus operationalStatus,
        Instant lastUpdatedAt,
        String occupancyStatus
) {}
