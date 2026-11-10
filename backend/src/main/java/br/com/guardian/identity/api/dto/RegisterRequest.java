package br.com.guardian.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 150, message = "O nome não pode exceder 150 caracteres.")
        String name,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail deve ser válido.")
        @Size(max = 255, message = "O e-mail não pode exceder 255 caracteres.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 12, message = "A senha deve ter no mínimo 12 caracteres.")
        String password
) {}
