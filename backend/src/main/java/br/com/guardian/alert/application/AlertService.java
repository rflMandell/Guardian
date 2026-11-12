package br.com.guardian.alert.application;

import br.com.guardian.accesscontrol.application.MunicipalScopeService;
import br.com.guardian.alert.api.dto.*;
import br.com.guardian.alert.domain.model.Alert;
import br.com.guardian.alert.domain.model.AlertArea;
import br.com.guardian.alert.domain.model.AlertOriginType;
import br.com.guardian.alert.domain.model.AlertStatus;
import br.com.guardian.alert.domain.repository.AlertRepository;
import br.com.guardian.geolocation.util.GeometryUtils;
import br.com.guardian.municipality.domain.repository.MunicipalityRepository;
import br.com.guardian.risk.domain.model.RiskLevel;
import br.com.guardian.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final MunicipalityRepository municipalityRepository;
    private final MunicipalScopeService municipalScopeService;

    public AlertService(AlertRepository alertRepository,
                        MunicipalityRepository municipalityRepository,
                        MunicipalScopeService municipalScopeService) {
        this.alertRepository = alertRepository;
        this.municipalityRepository = municipalityRepository;
        this.municipalScopeService = municipalScopeService;
    }

    @Transactional
    public AlertResponseData createAlert(CreateAlertRequest request, UUID operatorId) {
        if (operatorId != null) {
            municipalScopeService.validateScope(operatorId, request.municipalityId());
        }

        municipalityRepository.findById(request.municipalityId())
                .orElseThrow(() -> new ResourceNotFoundException("Município", request.municipalityId()));

        Alert alert = new Alert(
                request.municipalityId(),
                request.title(),
                request.description(),
                request.severity(),
                request.originType() != null ? request.originType() : AlertOriginType.GUARDIAN,
                operatorId,
                request.startsAt(),
                request.expiresAt()
        );

        if (request.incidentId() != null) {
            alert.setIncidentId(request.incidentId());
        }
        if (request.riskAssessmentId() != null) {
            alert.setRiskAssessmentId(request.riskAssessmentId());
        }

        if (request.areaDescription() != null) {
            AlertArea area = new AlertArea(GeometryUtils.createMultiPolygon(), request.areaDescription());
            alert.addArea(area);
        }

        Alert saved = alertRepository.save(alert);
        return toResponseData(saved, request.recommendations());
    }

    @Transactional
    public AlertResponseData updateAlert(UUID alertId, UpdateAlertRequest request, UUID operatorId) {
        Alert alert = findById(alertId);

        if (operatorId != null) {
            municipalScopeService.validateScope(operatorId, alert.getMunicipalityId());
        }

        alert.updateDetails(
                request.severity(),
                request.description(),
                request.expiresAt(),
                operatorId,
                request.reason() != null ? request.reason() : "Atualização de parâmetros operacionais do alerta."
        );

        Alert updated = alertRepository.save(alert);
        return toResponseData(updated, request.recommendations());
    }

    @Transactional
    public void closeAlert(UUID alertId, CloseAlertRequest request, UUID operatorId) {
        Alert alert = findById(alertId);

        if (operatorId != null) {
            municipalScopeService.validateScope(operatorId, alert.getMunicipalityId());
        }

        alert.close(operatorId, request.reason());
        alertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public Page<AlertResponseData> listAlerts(
            UUID municipalityId,
            RiskLevel severity,
            AlertStatus status,
            Pageable pageable
    ) {
        Specification<Alert> spec = Specification.where(null);

        if (municipalityId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("municipalityId"), municipalityId));
        }
        if (severity != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("severity"), severity));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        } else {
            // Por padrão, lista alertas ativos (PUBLISHED ou UPDATED que não expiraram)
            spec = spec.and((root, query, cb) -> root.get("status").in(AlertStatus.PUBLISHED, AlertStatus.UPDATED));
        }

        return alertRepository.findAll(spec, pageable)
                .map(a -> toResponseData(a, getDefaultRecommendations(a.getSeverity())));
    }

    @Transactional(readOnly = true)
    public AlertResponseData getAlert(UUID alertId) {
        Alert alert = findById(alertId);
        return toResponseData(alert, getDefaultRecommendations(alert.getSeverity()));
    }

    private Alert findById(UUID id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta", id));
    }

    private AlertResponseData toResponseData(Alert a, List<String> customRecommendations) {
        List<AlertAreaDto> areaDtos = a.getAreas().stream()
                .map(area -> new AlertAreaDto(area.getId(), area.getDescription()))
                .toList();

        List<String> recommendations = (customRecommendations != null && !customRecommendations.isEmpty())
                ? customRecommendations
                : getDefaultRecommendations(a.getSeverity());

        String dataStatus = a.isExpired() ? "EXPIRED" : "CURRENT";

        return new AlertResponseData(
                a.getId(),
                a.getMunicipalityId(),
                a.getTitle(),
                a.getMessage(),
                a.getSeverity(),
                a.getOriginType(),
                a.getStatus(),
                a.getIssuedAt(),
                a.getExpiresAt(),
                dataStatus,
                areaDtos,
                recommendations
        );
    }

    private List<String> getDefaultRecommendations(RiskLevel severity) {
        if (severity == RiskLevel.CRITICAL) {
            return List.of(
                    "Evite imediatamente a região afetada.",
                    "Siga rigorosamente as orientações das autoridades de emergência e Defesa Civil.",
                    "Dirija-se ao ponto de segurança ou abrigo mais próximo."
            );
        } else if (severity == RiskLevel.HIGH) {
            return List.of(
                    "Evite deslocamentos não essenciais pela região.",
                    "Não atravesse vias ou pontes alagadas.",
                    "Mantenha-se informado através dos canais oficiais."
            );
        } else if (severity == RiskLevel.MODERATE) {
            return List.of(
                    "Atenção redobrada para condições climáticas adversas.",
                    "Evite áreas historicamente sujeitas a inundações."
            );
        }
        return List.of("Monitore as atualizações preventivas da Defesa Civil.");
    }
}
