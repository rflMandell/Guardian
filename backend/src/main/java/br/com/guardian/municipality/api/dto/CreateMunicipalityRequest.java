package br.com.guardian.municipality.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateMunicipalityRequest(
        @NotBlank(message = "O nome do município é obrigatório.")
        @Size(max = 150, message = "O nome não pode exceder 150 caracteres.")
        String name,

        @NotBlank(message = "A sigla do estado (UF) é obrigatória.")
        @Size(min = 2, max = 2, message = "A sigla do estado deve possuir exatamente 2 caracteres.")
        String state,

        @NotBlank(message = "O código IBGE é obrigatório.")
        @Pattern(regexp = "\\d{7}", message = "O código IBGE deve conter exatamente 7 dígitos numéricos.")
        String ibgeCode,

        @Size(min = 2, max = 2, message = "O código do país deve possuir 2 caracteres.")
        String country
) {}
