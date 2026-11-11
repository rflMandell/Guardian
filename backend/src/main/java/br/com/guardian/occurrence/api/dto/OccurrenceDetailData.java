package br.com.guardian.occurrence.api.dto;

import br.com.guardian.occurrence.domain.model.EventCategory;
import br.com.guardian.occurrence.domain.model.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public record OccurrenceDetailData(
        UUID id,
        EventCategory eventCategory,
        String description,
        CoordinatesDto location,
        ReportStatus status,
        boolean anonymous,
        UUID municipalityId,
        Instant reportedAt,
        Instant createdAt,
        int potentialDuplicatesCount
) {}
