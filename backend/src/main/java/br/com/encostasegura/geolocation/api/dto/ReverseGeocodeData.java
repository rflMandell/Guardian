package br.com.encostasegura.geolocation.api.dto;

public record ReverseGeocodeData(
        String address,
        String city,
        String state,
        String postalCode,
        String country
) {}
