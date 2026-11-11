package br.com.guardian.occurrence.domain.model;

public enum ReportStatus {
    RECEIVED("Recebido pelo sistema"),
    UNDER_ANALYSIS("Em análise operacional"),
    LINKED_TO_INCIDENT("Associado a um incidente confirmado"),
    DISMISSED("Descartado após avaliação"),
    CLOSED("Encerrado");

    private final String description;

    ReportStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
