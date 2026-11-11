package br.com.guardian.occurrence.domain.model;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "municipality_id", nullable = false)
    private UUID municipalityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventCategory eventType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "location", nullable = false, columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "OPEN";

    @Column(name = "severity", nullable = false, length = 20)
    private String severity = "MODERATE";

    @Column(name = "origin", nullable = false, length = 20)
    private String origin = "CITIZEN_REPORTS";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    protected Incident() {}

    public Incident(UUID municipalityId, EventCategory eventType, String title,
                    String description, Point location, String severity, String origin) {
        this.municipalityId = Objects.requireNonNull(municipalityId, "Município é obrigatório.");
        this.eventType = Objects.requireNonNull(eventType, "Tipo de evento é obrigatório.");
        this.title = Objects.requireNonNull(title, "Título é obrigatório.");
        this.description = description;
        this.location = Objects.requireNonNull(location, "Localização é obrigatória.");
        this.severity = severity != null ? severity : "MODERATE";
        this.origin = origin != null ? origin : "CITIZEN_REPORTS";
        this.status = "OPEN";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getMunicipalityId() {
        return municipalityId;
    }

    public EventCategory getEventType() {
        return eventType;
    }

    public void setEventType(EventCategory eventType) {
        this.eventType = eventType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void close() {
        this.status = "CLOSED";
        this.closedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Incident incident)) return false;
        return id != null && Objects.equals(id, incident.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
