package br.com.guardian.risk.domain.model;

import br.com.guardian.occurrence.domain.model.EventCategory;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "risk_assessments")
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "municipality_id", nullable = false)
    private UUID municipalityId;

    @Column(name = "risk_area_id")
    private UUID riskAreaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventCategory eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", nullable = false, length = 20)
    private ConfidenceLevel confidenceLevel;

    @Column(name = "probability_percentage", precision = 5, scale = 2)
    private BigDecimal probabilityPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_status", nullable = false, length = 30)
    private AssessmentStatus assessmentStatus;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "engine_version", nullable = false, length = 30)
    private String engineVersion;

    @OneToMany(mappedBy = "riskAssessment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<RiskFactor> factors = new ArrayList<>();

    protected RiskAssessment() {}

    public RiskAssessment(UUID municipalityId, UUID riskAreaId, EventCategory eventType,
                          RiskLevel riskLevel, ConfidenceLevel confidenceLevel,
                          AssessmentStatus assessmentStatus, Instant evaluatedAt,
                          Instant validUntil, String engineVersion) {
        this.municipalityId = Objects.requireNonNull(municipalityId, "Município é obrigatório.");
        this.riskAreaId = riskAreaId;
        this.eventType = Objects.requireNonNull(eventType, "Tipo de evento é obrigatório.");
        this.riskLevel = Objects.requireNonNull(riskLevel, "Nível de risco é obrigatório.");
        this.confidenceLevel = Objects.requireNonNull(confidenceLevel, "Nível de confiança é obrigatório.");
        this.assessmentStatus = Objects.requireNonNull(assessmentStatus, "Status da avaliação é obrigatório.");
        this.evaluatedAt = evaluatedAt != null ? evaluatedAt : Instant.now();
        this.validUntil = validUntil;
        this.engineVersion = engineVersion != null ? engineVersion : "v1.0.0-rules";
        this.probabilityPercentage = null; // RN020: Nulo na ausência de modelo estatístico validado
    }

    public void addFactor(RiskFactor factor) {
        factors.add(factor);
        factor.setRiskAssessment(this);
    }

    public boolean isStale() {
        return validUntil != null && Instant.now().isAfter(validUntil);
    }

    public UUID getId() {
        return id;
    }

    public UUID getMunicipalityId() {
        return municipalityId;
    }

    public UUID getRiskAreaId() {
        return riskAreaId;
    }

    public EventCategory getEventType() {
        return eventType;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public ConfidenceLevel getConfidenceLevel() {
        return confidenceLevel;
    }

    public BigDecimal getProbabilityPercentage() {
        return probabilityPercentage;
    }

    public void setProbabilityPercentage(BigDecimal probabilityPercentage) {
        this.probabilityPercentage = probabilityPercentage;
    }

    public AssessmentStatus getAssessmentStatus() {
        return assessmentStatus;
    }

    public void setAssessmentStatus(AssessmentStatus assessmentStatus) {
        this.assessmentStatus = assessmentStatus;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public List<RiskFactor> getFactors() {
        return factors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RiskAssessment that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
