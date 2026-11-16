package br.com.guardian.identity.api.dto;

public record AuthTokensDto(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {}
