package br.com.guardian.alert.domain.model;

import br.com.guardian.risk.domain.model.RiskLevel;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "municipality_id", nullable = false)
    private UUID municipalityId;

    @Column(name = "incident_id")
    private UUID incidentId;

    @Column(name = "risk_assessment_id")
    private UUID riskAssessmentId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private RiskLevel severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_type", nullable = false, length = 30)
    private AlertOriginType originType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status = AlertStatus.DRAFT;

    @Column(name = "published_by")
    private UUID publishedBy;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<AlertArea> areas = new ArrayList<>();

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AlertStatusHistory> statusHistory = new ArrayList<>();

    protected Alert() {}

    public Alert(UUID municipalityId, String title, String message, RiskLevel severity,
                 AlertOriginType originType, UUID publishedBy, Instant issuedAt, Instant expiresAt) {
        this.municipalityId = Objects.requireNonNull(municipalityId, "Município é obrigatório.");
        this.title = Objects.requireNonNull(title, "Título é obrigatório.");
        this.message = Objects.requireNonNull(message, "Mensagem é obrigatória.");
        this.severity = Objects.requireNonNull(severity, "Severidade é obrigatória.");
        this.originType = originType != null ? originType : AlertOriginType.GUARDIAN;
        this.publishedBy = publishedBy;
        this.issuedAt = issuedAt != null ? issuedAt : Instant.now();
        this.expiresAt = expiresAt;
        this.status = AlertStatus.PUBLISHED;
        this.createdAt = Instant.now();

        // Registra histórico inicial
        this.addStatusHistory(AlertStatus.DRAFT, AlertStatus.PUBLISHED, publishedBy, "Alerta criado e publicado.");
    }

    public void addArea(AlertArea area) {
        areas.add(area);
        area.setAlert(this);
    }

    public void addStatusHistory(AlertStatus from, AlertStatus to, UUID changedBy, String reason) {
        AlertStatusHistory history = new AlertStatusHistory(this, from, to, changedBy, reason);
        statusHistory.add(history);
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return (status == AlertStatus.PUBLISHED || status == AlertStatus.UPDATED) && !isExpired();
    }

    public void close(UUID operatorId, String reason) {
        AlertStatus previous = this.status;
        this.status = AlertStatus.CLOSED;
        addStatusHistory(previous, AlertStatus.CLOSED, operatorId, reason);
    }

    public void updateDetails(RiskLevel newSeverity, String newMessage, Instant newExpiresAt, UUID operatorId, String reason) {
        AlertStatus previous = this.status;
        if (newSeverity != null) {
            this.severity = newSeverity;
        }
        if (newMessage != null) {
            this.message = newMessage;
        }
        if (newExpiresAt != null) {
            this.expiresAt = newExpiresAt;
        }
        this.status = AlertStatus.UPDATED;
        addStatusHistory(previous, AlertStatus.UPDATED, operatorId, reason);
    }

    public UUID getId() {
        return id;
    }

    public UUID getMunicipalityId() {
        return municipalityId;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(UUID incidentId) {
        this.incidentId = incidentId;
    }

    public UUID getRiskAssessmentId() {
        return riskAssessmentId;
    }

    public void setRiskAssessmentId(UUID riskAssessmentId) {
        this.riskAssessmentId = riskAssessmentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public RiskLevel getSeverity() {
        return severity;
    }

    public void setSeverity(RiskLevel severity) {
        this.severity = severity;
    }

    public AlertOriginType getOriginType() {
        return originType;
    }

    public void setOriginType(AlertOriginType originType) {
        this.originType = originType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
    }

    public AlertStatus getStatus() {
        if (isExpired() && (status == AlertStatus.PUBLISHED || status == AlertStatus.UPDATED)) {
            return AlertStatus.EXPIRED;
        }
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public UUID getPublishedBy() {
        return publishedBy;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<AlertArea> getAreas() {
        return areas;
    }

    public List<AlertStatusHistory> getStatusHistory() {
        return statusHistory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Alert alert)) return false;
        return id != null && Objects.equals(id, alert.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
