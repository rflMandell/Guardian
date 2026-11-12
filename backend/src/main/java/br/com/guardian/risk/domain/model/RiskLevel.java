package br.com.guardian.risk.domain.model;

public enum RiskLevel {
    LOW("Baixo"),
    MODERATE("Moderado"),
    HIGH("Alto"),
    CRITICAL("Crítico");

    private final String description;

    RiskLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
