package br.com.guardian.geolocation.api.dto;

public record ReverseGeocodeData(
        String address,
        String city,
        String state,
        String postalCode,
        String country
) {}
