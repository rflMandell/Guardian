package br.com.guardian.identity.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "O refresh token é obrigatório.")
        String refreshToken
) {}
