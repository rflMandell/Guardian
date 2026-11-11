package br.com.guardian.occurrence.domain.model;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "occurrence_reports")
public class OccurrenceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "municipality_id", nullable = false)
    private UUID municipalityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventCategory eventType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "location", nullable = false, columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private ReportStatus status = ReportStatus.RECEIVED;

    @Column(name = "reliability_score", precision = 5, scale = 2)
    private BigDecimal reliabilityScore;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected OccurrenceReport() {}

    public OccurrenceReport(UUID userId, UUID municipalityId, EventCategory eventType,
                            String description, Point location, Instant reportedAt) {
        this.userId = userId;
        this.municipalityId = Objects.requireNonNull(municipalityId, "Município é obrigatório.");
        this.eventType = Objects.requireNonNull(eventType, "Tipo de evento é obrigatório.");
        this.description = description;
        this.location = Objects.requireNonNull(location, "Localização é obrigatória.");
        this.reportedAt = reportedAt != null ? reportedAt : Instant.now();
        this.status = ReportStatus.RECEIVED;
        this.createdAt = Instant.now();
    }

    public boolean isAnonymous() {
        return this.userId == null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public BigDecimal getReliabilityScore() {
        return reliabilityScore;
    }

    public void setReliabilityScore(BigDecimal reliabilityScore) {
        this.reliabilityScore = reliabilityScore;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OccurrenceReport that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
