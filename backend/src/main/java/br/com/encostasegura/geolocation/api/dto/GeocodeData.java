package br.com.encostasegura.geolocation.api.dto;

public record GeocodeData(
        Double latitude,
        Double longitude,
        String formattedAddress
) {}
