package br.com.encostasegura.identity.api.dto;

import java.util.List;
import java.util.UUID;

public record LoginUserData(
        UUID id,
        String name,
        String email,
        String role,
        List<String> roles,
        UUID municipalityId,
        List<String> permissions
) {
    public LoginUserData(UUID id, String name, List<String> roles) {
        this(id, name, null, roles != null && !roles.isEmpty() ? roles.get(0) : null, roles, null, List.of());
    }
}
