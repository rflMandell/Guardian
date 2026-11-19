package br.com.encostasegura.geolocation.api.dto;

public record ResolveLocationRequest(
        Double latitude,
        Double longitude,
        String postalCode,
        String country
) {}
