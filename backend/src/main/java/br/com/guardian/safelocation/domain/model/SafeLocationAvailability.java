package br.com.guardian.safelocation.domain.model;

public enum SafeLocationAvailability {
    AVAILABLE("Disponível"),
    FULL("Lotado"),
    UNAVAILABLE("Indisponível / Fechado"),
    UNKNOWN("Disponibilidade desconhecida");

    private final String description;

    SafeLocationAvailability(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
