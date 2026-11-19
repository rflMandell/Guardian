package br.com.encostasegura.alert.domain.model;

public enum AlertOriginType {
    ENCOSTA_SEGURA("Alerta emitido pela plataforma Encosta Segura"),
    OFFICIAL_SOURCE("Alerta emitido por autoridade oficial (Defesa Civil / CEMADEN)"),
    RISK_ESTIMATE("Alerta preventivo baseado em risco estimado"),
    OCCURRENCE_RELATED("Alerta associado a ocorrência confirmada");

    private final String description;

    AlertOriginType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
