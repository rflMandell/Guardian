package br.com.guardian.safelocation.domain.model;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "safe_locations")
public class SafeLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "municipality_id", nullable = false)
    private UUID municipalityId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "location", nullable = false, columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Column(name = "address", length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 30)
    private SafeLocationType locationType;

    @Column(name = "maximum_capacity")
    private Integer maximumCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false, length = 30)
    private SafeLocationOperationalStatus operationalStatus = SafeLocationOperationalStatus.UNKNOWN;

    @Column(name = "accessibility_info", columnDefinition = "TEXT")
    private String accessibilityInfo;

    @Column(name = "operating_hours", columnDefinition = "jsonb")
    private String operatingHours;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected SafeLocation() {}

    public SafeLocation(UUID municipalityId, String name, String description, Point location,
                        String address, SafeLocationType locationType, Integer maximumCapacity,
                        SafeLocationOperationalStatus operationalStatus, String accessibilityInfo) {
        this.municipalityId = Objects.requireNonNull(municipalityId, "Município é obrigatório.");
        this.name = Objects.requireNonNull(name, "Nome do ponto seguro é obrigatório.");
        this.location = Objects.requireNonNull(location, "Localização é obrigatória.");
        this.description = description;
        this.address = address;
        this.locationType = locationType != null ? locationType : SafeLocationType.SHELTER;
        this.maximumCapacity = maximumCapacity;
        this.operationalStatus = operationalStatus != null ? operationalStatus : SafeLocationOperationalStatus.UNKNOWN;
        this.accessibilityInfo = accessibilityInfo;
        this.lastVerifiedAt = Instant.now();
        this.createdAt = Instant.now();
    }

    /**
     * RN042 e Regra de Segurança:
     * Um local com ocupação desconhecida, status desatualizado ou fechado NÃO poderá
     * ser apresentado como seguramente disponível (AVAILABLE).
     */
    public SafeLocationAvailability computeAvailability(Integer currentOccupancy) {
        if (operationalStatus == SafeLocationOperationalStatus.CLOSED) {
            return SafeLocationAvailability.UNAVAILABLE;
        }

        if (operationalStatus == SafeLocationOperationalStatus.UNKNOWN) {
            return SafeLocationAvailability.UNKNOWN;
        }

        if (currentOccupancy == null) {
            return SafeLocationAvailability.UNKNOWN;
        }

        if (maximumCapacity != null && currentOccupancy >= maximumCapacity) {
            return SafeLocationAvailability.FULL;
        }

        if (operationalStatus.isOpen()) {
            return SafeLocationAvailability.AVAILABLE;
        }

        return SafeLocationAvailability.UNKNOWN;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMunicipalityId() {
        return municipalityId;
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

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public SafeLocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(SafeLocationType locationType) {
        this.locationType = locationType;
    }

    public Integer getMaximumCapacity() {
        return maximumCapacity;
    }

    public void setMaximumCapacity(Integer maximumCapacity) {
        this.maximumCapacity = maximumCapacity;
    }

    public SafeLocationOperationalStatus getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(SafeLocationOperationalStatus operationalStatus) {
        this.operationalStatus = operationalStatus;
        this.lastVerifiedAt = Instant.now();
    }

    public String getAccessibilityInfo() {
        return accessibilityInfo;
    }

    public void setAccessibilityInfo(String accessibilityInfo) {
        this.accessibilityInfo = accessibilityInfo;
    }

    public String getOperatingHours() {
        return operatingHours;
    }

    public void setOperatingHours(String operatingHours) {
        this.operatingHours = operatingHours;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
    }

    public Instant getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(Instant lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafeLocation that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
