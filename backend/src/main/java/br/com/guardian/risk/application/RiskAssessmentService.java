package br.com.guardian.risk.application;

import br.com.guardian.accesscontrol.application.MunicipalScopeService;
import br.com.guardian.geolocation.domain.model.RiskArea;
import br.com.guardian.geolocation.domain.repository.RiskAreaRepository;
import br.com.guardian.municipality.domain.model.Municipality;
import br.com.guardian.municipality.domain.repository.MunicipalityRepository;
import br.com.guardian.occurrence.domain.model.EventCategory;
import br.com.guardian.occurrence.domain.model.Incident;
import br.com.guardian.occurrence.domain.model.OccurrenceReport;
import br.com.guardian.occurrence.domain.repository.IncidentRepository;
import br.com.guardian.occurrence.domain.repository.OccurrenceReportRepository;
import br.com.guardian.risk.api.dto.RecalculateResponseData;
import br.com.guardian.risk.api.dto.RecalculateRiskRequest;
import br.com.guardian.risk.api.dto.RiskAssessmentResponseData;
import br.com.guardian.risk.api.dto.RiskFactorData;
import br.com.guardian.risk.domain.model.AssessmentStatus;
import br.com.guardian.risk.domain.model.RiskAssessment;
import br.com.guardian.risk.domain.model.RiskFactor;
import br.com.guardian.risk.domain.model.RiskLevel;
import br.com.guardian.risk.domain.repository.RiskAssessmentRepository;
import br.com.guardian.shared.exception.BusinessException;
import br.com.guardian.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RiskAssessmentService {

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskAreaRepository riskAreaRepository;
    private final MunicipalityRepository municipalityRepository;
    private final OccurrenceReportRepository reportRepository;
    private final IncidentRepository incidentRepository;
    private final MunicipalScopeService municipalScopeService;
    private final RiskAssessmentEngine riskAssessmentEngine;

    public RiskAssessmentService(
            RiskAssessmentRepository riskAssessmentRepository,
            RiskAreaRepository riskAreaRepository,
            MunicipalityRepository municipalityRepository,
            OccurrenceReportRepository reportRepository,
            IncidentRepository incidentRepository,
            MunicipalScopeService municipalScopeService,
            RiskAssessmentEngine riskAssessmentEngine
    ) {
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.riskAreaRepository = riskAreaRepository;
        this.municipalityRepository = municipalityRepository;
        this.reportRepository = reportRepository;
        this.incidentRepository = incidentRepository;
        this.municipalScopeService = municipalScopeService;
        this.riskAssessmentEngine = riskAssessmentEngine;
    }

    @Transactional
    public RecalculateResponseData recalculateRisk(RecalculateRiskRequest request, UUID operatorId) {
        if (operatorId != null) {
            municipalScopeService.validateScope(operatorId, request.municipalityId());
        }

        Municipality municipality = municipalityRepository.findById(request.municipalityId())
                .orElseThrow(() -> new ResourceNotFoundException("Município", request.municipalityId()));

        RiskArea riskArea = null;
        if (request.riskAreaId() != null) {
            riskArea = riskAreaRepository.findById(request.riskAreaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Área de Risco", request.riskAreaId()));

            if (!riskArea.getMunicipality().getId().equals(municipality.getId())) {
                throw new BusinessException("MUNICIPALITY_MISMATCH",
                        "A área de risco informada não pertence ao município selecionado.");
            }
        }

        Instant since = Instant.now().minus(Duration.ofHours(4));
        List<OccurrenceReport> recentReports = reportRepository.findRecentReportsByMunicipalityAndType(
                municipality.getId(), request.eventCategory(), since
        );

        List<Incident> activeIncidents = incidentRepository.findByMunicipalityIdAndStatusNot(
                municipality.getId(), "CLOSED"
        ).stream().filter(i -> i.getEventType() == request.eventCategory()).toList();

        RiskAssessment assessment = riskAssessmentEngine.evaluate(
                municipality, riskArea, request.eventCategory(), recentReports, activeIncidents
        );

        RiskAssessment saved = riskAssessmentRepository.save(assessment);

        return new RecalculateResponseData(
                "QUEUED",
                "Avaliação solicitada para processamento.",
                saved.getId()
        );
    }

    @Transactional(readOnly = true)
    public Page<RiskAssessmentResponseData> listAssessments(
            UUID municipalityId,
            UUID riskAreaId,
            EventCategory eventCategory,
            RiskLevel riskLevel,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        Specification<RiskAssessment> spec = Specification.where(null);

        if (municipalityId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("municipalityId"), municipalityId));
        }
        if (riskAreaId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("riskAreaId"), riskAreaId));
        }
        if (eventCategory != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("eventType"), eventCategory));
        }
        if (riskLevel != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("riskLevel"), riskLevel));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("evaluatedAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("evaluatedAt"), to));
        }

        return riskAssessmentRepository.findAll(spec, pageable).map(this::toResponseData);
    }

    @Transactional(readOnly = true)
    public RiskAssessmentResponseData getAssessment(UUID assessmentId) {
        RiskAssessment assessment = riskAssessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação de Risco", assessmentId));

        return toResponseData(assessment);
    }

    @Transactional(readOnly = true)
    public RiskAssessmentResponseData getLatestAssessment(UUID municipalityId, EventCategory eventCategory) {
        RiskAssessment assessment;
        if (eventCategory != null) {
            assessment = riskAssessmentRepository
                    .findFirstByMunicipalityIdAndEventTypeOrderByEvaluatedAtDesc(municipalityId, eventCategory)
                    .orElseThrow(() -> new ResourceNotFoundException("Avaliação de Risco para a categoria", municipalityId));
        } else {
            assessment = riskAssessmentRepository
                    .findFirstByMunicipalityIdOrderByEvaluatedAtDesc(municipalityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Avaliação de Risco do município", municipalityId));
        }

        return toResponseData(assessment);
    }

    public RiskAssessmentResponseData toResponseData(RiskAssessment a) {
        String dataStatus = a.isStale() ? AssessmentStatus.STALE_DATA.name() : a.getAssessmentStatus().name();

        List<RiskFactorData> factorData = a.getFactors().stream().map(f -> new RiskFactorData(
                f.getId(),
                f.getFactorType(),
                formatFactorName(f.getFactorType()),
                f.getWeight() != null && f.getWeight().doubleValue() >= 0.5 ? "INCREASES_RISK" : "NEUTRAL",
                f.getDescription(),
                f.getWeight()
        )).toList();

        return new RiskAssessmentResponseData(
                a.getId(),
                a.getMunicipalityId(),
                a.getRiskAreaId(),
                a.getRiskLevel(),
                a.getEventType(),
                a.getConfidenceLevel(),
                a.getProbabilityPercentage(),
                dataStatus,
                factorData,
                a.getEvaluatedAt(),
                a.getValidUntil()
        );
    }

    private String formatFactorName(String factorType) {
        return switch (factorType) {
            case "HISTORICAL_VULNERABILITY" -> "Vulnerabilidade territorial";
            case "ACTIVE_INCIDENT" -> "Incidentes ativos confirmados";
            case "CITIZEN_REPORTS" -> "Relatos de cidadãos";
            case "DATA_SUFFICIENCY" -> "Disponibilidade de dados";
            default -> "Fator de risco";
        };
    }
}
