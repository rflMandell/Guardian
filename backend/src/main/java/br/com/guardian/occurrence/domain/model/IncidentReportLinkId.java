package br.com.guardian.occurrence.domain.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class IncidentReportLinkId implements Serializable {

    private UUID incidentId;
    private UUID reportId;

    public IncidentReportLinkId() {}

    public IncidentReportLinkId(UUID incidentId, UUID reportId) {
        this.incidentId = incidentId;
        this.reportId = reportId;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public UUID getReportId() {
        return reportId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IncidentReportLinkId that)) return false;
        return Objects.equals(incidentId, that.incidentId) && Objects.equals(reportId, that.reportId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(incidentId, reportId);
    }
}
