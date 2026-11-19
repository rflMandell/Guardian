package br.com.encostasegura.alert.domain.model;

public enum AlertStatus {
    DRAFT("Rascunho"),
    PUBLISHED("Publicado / Ativo"),
    UPDATED("Atualizado"),
    EXPIRED("Expirado"),
    CANCELLED("Cancelado"),
    CLOSED("Encerrado");

    private final String description;

    AlertStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return this == PUBLISHED || this == UPDATED;
    }
}
