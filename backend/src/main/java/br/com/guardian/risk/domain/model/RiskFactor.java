package br.com.guardian.risk.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "risk_factors")
public class RiskFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_assessment_id", nullable = false)
    @JsonIgnore
    private RiskAssessment riskAssessment;

    @Column(name = "factor_type", nullable = false, length = 50)
    private String factorType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "observed_value", columnDefinition = "jsonb")
    private String observedValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected RiskFactor() {}

    public RiskFactor(String factorType, String description, BigDecimal weight, String observedValue) {
        this.factorType = Objects.requireNonNull(factorType, "Tipo de fator é obrigatório.");
        this.description = Objects.requireNonNull(description, "Descrição é obrigatória.");
        this.weight = weight;
        this.observedValue = observedValue;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public RiskAssessment getRiskAssessment() {
        return riskAssessment;
    }

    public void setRiskAssessment(RiskAssessment riskAssessment) {
        this.riskAssessment = riskAssessment;
    }

    public String getFactorType() {
        return factorType;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public String getObservedValue() {
        return observedValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RiskFactor that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
