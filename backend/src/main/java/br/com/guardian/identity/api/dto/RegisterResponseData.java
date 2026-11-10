package br.com.guardian.identity.api.dto;

import java.util.UUID;

public record RegisterResponseData(
        UUID id,
        String name,
        String email,
        String accountType,
        boolean emailVerified
) {}
