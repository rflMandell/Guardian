package br.com.encostasegura.occurrence.api.dto;

import br.com.encostasegura.occurrence.domain.model.EventCategory;
import br.com.encostasegura.occurrence.domain.model.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public record OccurrenceResponseData(
        UUID id,
        EventCategory eventCategory,
        ReportStatus status,
        boolean anonymous,
        UUID municipalityId,
        Instant receivedAt
) {}
