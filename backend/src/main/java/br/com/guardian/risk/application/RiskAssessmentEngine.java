package br.com.guardian.risk.application;

import br.com.guardian.geolocation.domain.model.RiskArea;
import br.com.guardian.municipality.domain.model.Municipality;
import br.com.guardian.occurrence.domain.model.EventCategory;
import br.com.guardian.occurrence.domain.model.Incident;
import br.com.guardian.occurrence.domain.model.OccurrenceReport;
import br.com.guardian.risk.domain.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class RiskAssessmentEngine {

    public static final String ENGINE_VERSION = "v1.0.0-rules";

    public RiskAssessment evaluate(
            Municipality municipality,
            RiskArea riskArea,
            EventCategory eventType,
            List<OccurrenceReport> recentReports,
            List<Incident> activeIncidents
    ) {
        Instant evaluatedAt = Instant.now();
        List<RiskFactor> factors = new ArrayList<>();

        RiskLevel currentRisk = RiskLevel.LOW;
        ConfidenceLevel confidence = ConfidenceLevel.MEDIUM;
        AssessmentStatus status = AssessmentStatus.VALID;

        // 1. Baseline de Vulnerabilidade Territorial (RiskArea)
        if (riskArea != null) {
            String vuln = riskArea.getVulnerabilityLevel() != null
                    ? riskArea.getVulnerabilityLevel().toUpperCase()
                    : "MODERATE";

            switch (vuln) {
                case "CRITICAL" -> {
                    currentRisk = RiskLevel.CRITICAL;
                    factors.add(new RiskFactor(
                            "HISTORICAL_VULNERABILITY",
                            String.format("Área classificada com vulnerabilidade territorial CRÍTICA para %s.", eventType.getDescription()),
                            new BigDecimal("0.90"),
                            String.format("{\"vulnerability\":\"CRITICAL\",\"areaId\":\"%s\"}", riskArea.getId())
                    ));
                }
                case "HIGH" -> {
                    currentRisk = RiskLevel.HIGH;
                    factors.add(new RiskFactor(
                            "HISTORICAL_VULNERABILITY",
                            String.format("Área classificada com vulnerabilidade territorial ALTA para %s.", eventType.getDescription()),
                            new BigDecimal("0.75"),
                            String.format("{\"vulnerability\":\"HIGH\",\"areaId\":\"%s\"}", riskArea.getId())
                    ));
                }
                case "MODERATE" -> {
                    currentRisk = RiskLevel.MODERATE;
                    factors.add(new RiskFactor(
                            "HISTORICAL_VULNERABILITY",
                            String.format("Área classificada com vulnerabilidade territorial MODERADA para %s.", eventType.getDescription()),
                            new BigDecimal("0.50"),
                            String.format("{\"vulnerability\":\"MODERATE\",\"areaId\":\"%s\"}", riskArea.getId())
                    ));
                }
                default -> factors.add(new RiskFactor(
                        "HISTORICAL_VULNERABILITY",
                        String.format("Área classificada com vulnerabilidade territorial BAIXA para %s.", eventType.getDescription()),
                        new BigDecimal("0.25"),
                        String.format("{\"vulnerability\":\"LOW\",\"areaId\":\"%s\"}", riskArea.getId())
                ));
            }
        }

        // 2. Incidentes Ativos Confirmados (RN017)
        if (activeIncidents != null && !activeIncidents.isEmpty()) {
            boolean hasCriticalIncident = activeIncidents.stream()
                    .anyMatch(i -> "CRITICAL".equalsIgnoreCase(i.getSeverity()));
            boolean hasHighIncident = activeIncidents.stream()
                    .anyMatch(i -> "HIGH".equalsIgnoreCase(i.getSeverity()));

            if (hasCriticalIncident) {
                currentRisk = RiskLevel.CRITICAL;
            } else if (hasHighIncident && currentRisk != RiskLevel.CRITICAL) {
                currentRisk = RiskLevel.HIGH;
            } else if (currentRisk == RiskLevel.LOW) {
                currentRisk = RiskLevel.MODERATE;
            }

            for (Incident incident : activeIncidents) {
                factors.add(new RiskFactor(
                        "ACTIVE_INCIDENT",
                        String.format("Incidente confirmado ativo no território: '%s' (Severidade: %s).",
                                incident.getTitle(), incident.getSeverity()),
                        new BigDecimal("0.95"),
                        String.format("{\"incidentId\":\"%s\",\"severity\":\"%s\"}",
                                incident.getId(), incident.getSeverity())
                ));
            }
        }

        // 3. Relatos Recentes de Cidadãos (RN019)
        int reportCount = recentReports != null ? recentReports.size() : 0;
        if (reportCount >= 5) {
            currentRisk = RiskLevel.CRITICAL;
            factors.add(new RiskFactor(
                    "CITIZEN_REPORTS",
                    String.format("Concentração crítica de relatos de cidadãos (%d relatos nas últimas 4 horas).", reportCount),
                    new BigDecimal("0.85"),
                    String.format("{\"reportCount\":%d,\"timeWindowHours\":4}", reportCount)
            ));
        } else if (reportCount >= 2) {
            if (currentRisk == RiskLevel.LOW) {
                currentRisk = RiskLevel.MODERATE;
            } else if (currentRisk == RiskLevel.MODERATE) {
                currentRisk = RiskLevel.HIGH;
            }
            factors.add(new RiskFactor(
                    "CITIZEN_REPORTS",
                    String.format("Múltiplos relatos recentes de cidadãos (%d relatos nas últimas 4 horas).", reportCount),
                    new BigDecimal("0.65"),
                    String.format("{\"reportCount\":%d,\"timeWindowHours\":4}", reportCount)
            ));
        } else if (reportCount == 1) {
            factors.add(new RiskFactor(
                    "CITIZEN_REPORTS",
                    "Relato pontual recente de cidadão no município.",
                    new BigDecimal("0.35"),
                    "{\"reportCount\":1,\"timeWindowHours\":4}"
            ));
        }

        // 4. Verificação de Dados Insuficientes (RN021 / RN022)
        if (riskArea == null && reportCount == 0 && (activeIncidents == null || activeIncidents.isEmpty())) {
            status = AssessmentStatus.INSUFFICIENT_DATA;
            confidence = ConfidenceLevel.LOW;
            factors.add(new RiskFactor(
                    "DATA_SUFFICIENCY",
                    "Dados territoriais e observações insuficientes para modelagem precisa.",
                    new BigDecimal("0.10"),
                    "{\"status\":\"INSUFFICIENT_DATA\"}"
            ));
        } else {
            status = AssessmentStatus.VALID;
            if ((activeIncidents != null && !activeIncidents.isEmpty()) || (riskArea != null && reportCount > 0)) {
                confidence = ConfidenceLevel.HIGH;
            } else {
                confidence = ConfidenceLevel.MEDIUM;
            }
        }

        // 5. Janela de Validade
        Duration validityDuration = switch (currentRisk) {
            case CRITICAL -> Duration.ofHours(1);
            case HIGH -> Duration.ofHours(2);
            case MODERATE -> Duration.ofHours(4);
            case LOW -> Duration.ofHours(8);
        };
        Instant validUntil = evaluatedAt.plus(validityDuration);

        RiskAssessment assessment = new RiskAssessment(
                municipality.getId(),
                riskArea != null ? riskArea.getId() : null,
                eventType,
                currentRisk,
                confidence,
                status,
                evaluatedAt,
                validUntil,
                ENGINE_VERSION
        );

        for (RiskFactor f : factors) {
            assessment.addFactor(f);
        }

        return assessment;
    }
}
