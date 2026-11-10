package br.com.guardian.identity.api.dto;

public record LoginResponseData(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        LoginUserData user
) {}
