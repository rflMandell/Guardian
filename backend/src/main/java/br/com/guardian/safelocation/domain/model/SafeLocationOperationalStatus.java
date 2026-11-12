package br.com.guardian.safelocation.domain.model;

public enum SafeLocationOperationalStatus {
    OPEN("Aberto / Em operação"),
    AVAILABLE("Disponível"),
    PARTIALLY_OCCUPIED("Parcialmente ocupado"),
    FULL("Lotado / Capacidade máxima atingida"),
    CLOSED("Fechado / Interditado"),
    UNKNOWN("Status operacional desconhecido");

    private final String description;

    SafeLocationOperationalStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isOpen() {
        return this == OPEN || this == AVAILABLE || this == PARTIALLY_OCCUPIED;
    }
}
