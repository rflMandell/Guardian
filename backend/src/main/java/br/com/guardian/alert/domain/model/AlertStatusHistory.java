package br.com.guardian.alert.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "alert_status_history")
public class AlertStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    @JsonIgnore
    private Alert alert;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 20)
    private AlertStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private AlertStatus toStatus;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt = Instant.now();

    protected AlertStatusHistory() {}

    public AlertStatusHistory(Alert alert, AlertStatus fromStatus, AlertStatus toStatus, UUID changedBy, String reason) {
        this.alert = alert;
        this.fromStatus = Objects.requireNonNull(fromStatus, "Status anterior é obrigatório.");
        this.toStatus = Objects.requireNonNull(toStatus, "Novo status é obrigatório.");
        this.changedBy = changedBy;
        this.reason = reason;
        this.changedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Alert getAlert() {
        return alert;
    }

    public AlertStatus getFromStatus() {
        return fromStatus;
    }

    public AlertStatus getToStatus() {
        return toStatus;
    }

    public UUID getChangedBy() {
        return changedBy;
    }

    public String getReason() {
        return reason;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlertStatusHistory that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
