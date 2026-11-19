package br.com.encostasegura.occurrence.api.dto;

import br.com.encostasegura.occurrence.domain.model.EventCategory;
import br.com.encostasegura.occurrence.domain.model.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public record OccurrenceSummaryData(
        UUID id,
        EventCategory eventCategory,
        ReportStatus status,
        boolean anonymous,
        UUID municipalityId,
        Double latitude,
        Double longitude,
        Instant reportedAt
) {}
