package br.com.encostasegura.safelocation.api.dto;

import br.com.encostasegura.occurrence.api.dto.CoordinatesDto;
import br.com.encostasegura.safelocation.domain.model.SafeLocationAvailability;
import br.com.encostasegura.safelocation.domain.model.SafeLocationOperationalStatus;
import br.com.encostasegura.safelocation.domain.model.SafeLocationType;

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
