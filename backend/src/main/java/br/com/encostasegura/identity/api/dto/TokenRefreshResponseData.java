package br.com.encostasegura.identity.api.dto;

public record TokenRefreshResponseData(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken
) {}
