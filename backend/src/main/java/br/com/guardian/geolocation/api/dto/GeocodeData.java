package br.com.guardian.geolocation.api.dto;

public record GeocodeData(
        Double latitude,
        Double longitude,
        String formattedAddress
) {}
