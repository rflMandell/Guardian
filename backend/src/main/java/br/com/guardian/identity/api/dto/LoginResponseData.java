package br.com.guardian.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponseData(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        LoginUserData user
) {
    @JsonProperty("tokens")
    public AuthTokensDto tokens() {
        return new AuthTokensDto(accessToken, refreshToken, tokenType, expiresIn);
    }
}
