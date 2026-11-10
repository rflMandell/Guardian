package br.com.guardian.municipality.domain.model;

import jakarta.persistence.*;
import org.locationtech.jts.geom.MultiPolygon;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "municipalities")
public class Municipality {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 2)
    private String state;

    @Column(name = "ibge_code", nullable = false, unique = true, length = 10)
    private String ibgeCode;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode = "BR";

    @Column(nullable = false, length = 60)
    private String timezone = "America/Sao_Paulo";

    @Column(columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon boundary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MunicipalityStatus status = MunicipalityStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Municipality() {}

    public Municipality(String name, String state, String ibgeCode) {
        this.name = name;
        this.state = state;
        this.ibgeCode = ibgeCode;
        this.countryCode = "BR";
        this.timezone = "America/Sao_Paulo";
        this.status = MunicipalityStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public Municipality(String name, String state, String ibgeCode, String countryCode) {
        this.name = name;
        this.state = state;
        this.ibgeCode = ibgeCode;
        this.countryCode = (countryCode != null && !countryCode.isBlank()) ? countryCode : "BR";
        this.timezone = "America/Sao_Paulo";
        this.status = MunicipalityStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getIbgeCode() {
        return ibgeCode;
    }

    public void setIbgeCode(String ibgeCode) {
        this.ibgeCode = ibgeCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public MultiPolygon getBoundary() {
        return boundary;
    }

    public void setBoundary(MultiPolygon boundary) {
        this.boundary = boundary;
    }

    public MunicipalityStatus getStatus() {
        return status;
    }

    public void setStatus(MunicipalityStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Municipality that = (Municipality) o;
        return Objects.equals(ibgeCode, that.ibgeCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ibgeCode);
    }
}
