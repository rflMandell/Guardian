package br.com.guardian.occurrence.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "incident_report_links")
@IdClass(IncidentReportLinkId.class)
public class IncidentReportLink {

    @Id
    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Id
    @Column(name = "report_id", nullable = false)
    private UUID reportId;

    @Column(name = "linking_method", nullable = false, length = 20)
    private String linkingMethod = "MANUAL";

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt = Instant.now();

    protected IncidentReportLink() {}

    public IncidentReportLink(UUID incidentId, UUID reportId, String linkingMethod) {
        this.incidentId = Objects.requireNonNull(incidentId, "Identificador do incidente é obrigatório.");
        this.reportId = Objects.requireNonNull(reportId, "Identificador do relato é obrigatório.");
        this.linkingMethod = linkingMethod != null ? linkingMethod : "MANUAL";
        this.linkedAt = Instant.now();
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public UUID getReportId() {
        return reportId;
    }

    public String getLinkingMethod() {
        return linkingMethod;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IncidentReportLink that)) return false;
        return Objects.equals(incidentId, that.incidentId) && Objects.equals(reportId, that.reportId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(incidentId, reportId);
    }
}
