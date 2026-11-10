package br.com.guardian.shared.api.dto;

import java.time.Instant;

public record HealthResponse(
        String status,
        String application,
        String version,
        Instant timestamp
) {
    public static HealthResponse up() {
        return new HealthResponse("UP", "Guardian API", "1.0.0", Instant.now());
    }
}
