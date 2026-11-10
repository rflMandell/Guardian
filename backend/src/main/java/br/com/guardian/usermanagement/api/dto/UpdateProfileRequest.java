package br.com.guardian.usermanagement.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 150, message = "O nome não pode exceder 150 caracteres.")
        String name,

        @Size(max = 5, message = "O idioma preferido deve ter até 5 caracteres (ex: pt-BR).")
        String preferredLanguage
) {}
