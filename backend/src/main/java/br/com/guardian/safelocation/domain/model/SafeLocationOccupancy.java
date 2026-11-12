package br.com.guardian.safelocation.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "safe_location_occupancies")
public class SafeLocationOccupancy {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "safe_location_id", nullable = false)
    @JsonIgnore
    private SafeLocation safeLocation;

    @Column(name = "occupancy_count", nullable = false)
    private Integer occupancyCount;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;

    @Column(name = "collected_at", nullable = false, updatable = false)
    private Instant collectedAt = Instant.now();

    protected SafeLocationOccupancy() {}

    public SafeLocationOccupancy(SafeLocation safeLocation, Integer occupancyCount, UUID sourceId, Instant reportedAt) {
        this.safeLocation = Objects.requireNonNull(safeLocation, "Ponto seguro é obrigatório.");
        this.occupancyCount = Objects.requireNonNull(occupancyCount, "Contagem de ocupação é obrigatória.");
        if (occupancyCount < 0) {
            throw new IllegalArgumentException("A ocupação não pode ser negativa.");
        }
        this.sourceId = sourceId;
        this.reportedAt = reportedAt != null ? reportedAt : Instant.now();
        this.collectedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public SafeLocation getSafeLocation() {
        return safeLocation;
    }

    public Integer getOccupancyCount() {
        return occupancyCount;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafeLocationOccupancy that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
