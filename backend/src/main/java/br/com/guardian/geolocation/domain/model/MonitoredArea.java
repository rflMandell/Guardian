package br.com.guardian.geolocation.domain.model;

import br.com.guardian.municipality.domain.model.Municipality;
import jakarta.persistence.*;
import org.locationtech.jts.geom.MultiPolygon;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "monitored_areas")
public class MonitoredArea {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "municipality_id", nullable = false)
    private Municipality municipality;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon geometry;

    @Column(nullable = false, length = 20)
    private String priority = "NORMAL";

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public MonitoredArea() {}

    public MonitoredArea(Municipality municipality, String name, String description, MultiPolygon geometry, String priority) {
        this.municipality = municipality;
        this.name = name;
        this.description = description;
        this.geometry = geometry;
        this.priority = priority != null ? priority : "NORMAL";
        this.active = true;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Municipality getMunicipality() {
        return municipality;
    }

    public void setMunicipality(Municipality municipality) {
        this.municipality = municipality;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MultiPolygon getGeometry() {
        return geometry;
    }

    public void setGeometry(MultiPolygon geometry) {
        this.geometry = geometry;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
