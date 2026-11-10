package br.com.guardian.identity.api.dto;

import java.util.List;
import java.util.UUID;

public record LoginUserData(
        UUID id,
        String name,
        List<String> roles
) {}
