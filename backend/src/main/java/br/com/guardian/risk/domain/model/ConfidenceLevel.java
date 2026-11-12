package br.com.guardian.risk.domain.model;

public enum ConfidenceLevel {
    LOW("Baixa confiabilidade"),
    MEDIUM("Média confiabilidade"),
    HIGH("Alta confiabilidade");

    private final String description;

    ConfidenceLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
