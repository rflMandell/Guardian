package br.com.guardian.geolocation.domain.model;

import br.com.guardian.municipality.domain.model.Municipality;
import jakarta.persistence.*;
import org.locationtech.jts.geom.MultiPolygon;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "risk_areas")
public class RiskArea {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "municipality_id", nullable = false)
    private Municipality municipality;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitored_area_id")
    private MonitoredArea monitoredArea;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon geometry;

    @Column(name = "vulnerability_level", nullable = false, length = 20)
    private String vulnerabilityLevel;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public RiskArea() {}

    public RiskArea(Municipality municipality, MonitoredArea monitoredArea, String eventType,
                    MultiPolygon geometry, String vulnerabilityLevel) {
        this.municipality = municipality;
        this.monitoredArea = monitoredArea;
        this.eventType = eventType;
        this.geometry = geometry;
        this.vulnerabilityLevel = vulnerabilityLevel;
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

    public MonitoredArea getMonitoredArea() {
        return monitoredArea;
    }

    public void setMonitoredArea(MonitoredArea monitoredArea) {
        this.monitoredArea = monitoredArea;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public MultiPolygon getGeometry() {
        return geometry;
    }

    public void setGeometry(MultiPolygon geometry) {
        this.geometry = geometry;
    }

    public String getVulnerabilityLevel() {
        return vulnerabilityLevel;
    }

    public void setVulnerabilityLevel(String vulnerabilityLevel) {
        this.vulnerabilityLevel = vulnerabilityLevel;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
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
