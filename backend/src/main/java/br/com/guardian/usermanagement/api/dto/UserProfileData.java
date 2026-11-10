package br.com.guardian.usermanagement.api.dto;

import java.util.List;
import java.util.UUID;

public record UserProfileData(
        UUID id,
        String name,
        String email,
        String accountType,
        String preferredLanguage,
        boolean emailVerified,
        List<String> roles
) {}
