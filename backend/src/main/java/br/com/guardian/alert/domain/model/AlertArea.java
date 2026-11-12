package br.com.guardian.alert.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.locationtech.jts.geom.MultiPolygon;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "alert_areas")
public class AlertArea {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    @JsonIgnore
    private Alert alert;

    @Column(nullable = false, columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon geometry;

    @Column(name = "description", length = 255)
    private String description;

    protected AlertArea() {}

    public AlertArea(MultiPolygon geometry, String description) {
        this.geometry = Objects.requireNonNull(geometry, "Geometria da área afetada é obrigatória.");
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public Alert getAlert() {
        return alert;
    }

    public void setAlert(Alert alert) {
        this.alert = alert;
    }

    public MultiPolygon getGeometry() {
        return geometry;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlertArea that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
