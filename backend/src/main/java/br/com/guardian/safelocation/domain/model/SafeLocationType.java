package br.com.guardian.safelocation.domain.model;

public enum SafeLocationType {
    SHELTER("Abrigo emergencial"),
    SCHOOL("Escola"),
    GYMNASIUM("Ginásio / Centro esportivo"),
    HOSPITAL("Hospital / Unidade de saúde"),
    COMMUNITY_CENTER("Centro comunitário"),
    OTHER("Outro ponto de apoio");

    private final String description;

    SafeLocationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
