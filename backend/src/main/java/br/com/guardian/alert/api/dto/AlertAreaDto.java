package br.com.guardian.alert.api.dto;

import java.util.UUID;

public record AlertAreaDto(
        UUID id,
        String description
) {}
