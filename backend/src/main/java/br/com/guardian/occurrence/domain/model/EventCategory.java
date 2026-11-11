package br.com.guardian.occurrence.domain.model;

public enum EventCategory {
    FLOODING("Alagamentos"),
    FLOOD("Enchentes"),
    LANDSLIDE("Deslizamentos de terra"),
    FLASH_FLOOD("Enxurradas"),
    HEAVY_RAIN("Chuvas intensas"),
    STORM_STRONG_WINDS("Tempestades e ventos fortes"),
    CONTAINMENT_FAILURE("Rompimento de estruturas de contenção");

    private final String description;

    EventCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
