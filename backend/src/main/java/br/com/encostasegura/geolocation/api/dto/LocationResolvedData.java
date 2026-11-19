package br.com.encostasegura.geolocation.api.dto;

import java.util.UUID;

public record LocationResolvedData(
        UUID municipalityId,
        String municipalityName,
        String state,
        Double latitude,
        Double longitude,
        String formattedAddress,
        String resolutionSource
) {}
