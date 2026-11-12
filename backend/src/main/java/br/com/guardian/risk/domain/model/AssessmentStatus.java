package br.com.guardian.risk.domain.model;

public enum AssessmentStatus {
    VALID("Avaliação válida e atualizada"),
    INSUFFICIENT_DATA("Dados insuficientes para avaliação precisa"),
    STALE_DATA("Avaliação baseada em dados desatualizados");

    private final String description;

    AssessmentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
