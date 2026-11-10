package br.com.guardian.municipality.api.dto;

import java.util.UUID;

public record MunicipalityResponseData(
        UUID id,
        String name,
        String state,
        String country,
        String ibgeCode,
        String status
) {}
