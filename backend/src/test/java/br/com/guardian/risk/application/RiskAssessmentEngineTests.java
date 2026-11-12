package br.com.guardian.risk.application;

import br.com.guardian.geolocation.domain.model.RiskArea;
import br.com.guardian.geolocation.util.GeometryUtils;
import br.com.guardian.municipality.domain.model.Municipality;
import br.com.guardian.occurrence.domain.model.EventCategory;
import br.com.guardian.occurrence.domain.model.Incident;
import br.com.guardian.occurrence.domain.model.OccurrenceReport;
import br.com.guardian.risk.domain.model.AssessmentStatus;
import br.com.guardian.risk.domain.model.ConfidenceLevel;
import br.com.guardian.risk.domain.model.RiskAssessment;
import br.com.guardian.risk.domain.model.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RiskAssessmentEngine - Testes unitários do motor de regras determinísticas de risco")
class RiskAssessmentEngineTests {

    private RiskAssessmentEngine engine;
    private Municipality municipality;

    @BeforeEach
    void setUp() {
        engine = new RiskAssessmentEngine();
        municipality = new Municipality("Petrópolis", "RJ", "3303906", "BR");
        ReflectionTestUtils.setField(municipality, "id", UUID.randomUUID());
    }

    @Test
    @DisplayName("RN018 / RN019: Deve avaliar risco como CRITICAL quando área de risco possuir vulnerabilidade territorial CRITICAL")
    void shouldEvaluateCriticalRiskFromTerritorialVulnerability() {
        RiskArea criticalArea = new RiskArea(
                municipality, null, "LANDSLIDE",
                GeometryUtils.createMultiPolygon(), "CRITICAL"
        );
        ReflectionTestUtils.setField(criticalArea, "id", UUID.randomUUID());

        RiskAssessment assessment = engine.evaluate(
                municipality, criticalArea, EventCategory.LANDSLIDE, List.of(), List.of()
        );

        assertThat(assessment.getRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(assessment.getAssessmentStatus()).isEqualTo(AssessmentStatus.VALID);
        assertThat(assessment.getFactors()).anyMatch(f -> "HISTORICAL_VULNERABILITY".equals(f.getFactorType())
                && f.getDescription().contains("CRÍTICA"));
        assertThat(assessment.getProbabilityPercentage()).isNull(); // RN020
    }

    @Test
    @DisplayName("RN019: Deve elevar risco para CRITICAL quando houver 5 ou mais relatos recentes de cidadãos")
    void shouldElevateRiskToCriticalWithMultipleCitizenReports() {
        Point point = GeometryUtils.createPoint(-22.505, -43.18);
        List<OccurrenceReport> reports = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            reports.add(new OccurrenceReport(
                    null, municipality.getId(), EventCategory.FLOODING,
                    "Alagamento grave " + i, point, Instant.now()
            ));
        }

        RiskAssessment assessment = engine.evaluate(
                municipality, null, EventCategory.FLOODING, reports, List.of()
        );

        assertThat(assessment.getRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(assessment.getFactors()).anyMatch(f -> "CITIZEN_REPORTS".equals(f.getFactorType())
                && f.getDescription().contains("5 relatos"));
    }

    @Test
    @DisplayName("RN017: Deve elevar risco e registrar fator quando houver incidente ativo confirmado")
    void shouldElevateRiskWhenActiveIncidentExists() {
        Incident activeIncident = new Incident(
                municipality.getId(), EventCategory.LANDSLIDE,
                "Deslizamento no Morro da Oficina", "Queda de barreira confirmada",
                GeometryUtils.createPoint(-22.50, -43.18), "HIGH", "DEFESA_CIVIL"
        );

        RiskAssessment assessment = engine.evaluate(
                municipality, null, EventCategory.LANDSLIDE, List.of(), List.of(activeIncident)
        );

        assertThat(assessment.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(assessment.getConfidenceLevel()).isEqualTo(ConfidenceLevel.HIGH);
        assertThat(assessment.getFactors()).anyMatch(f -> "ACTIVE_INCIDENT".equals(f.getFactorType())
                && f.getDescription().contains("Deslizamento no Morro da Oficina"));
    }

    @Test
    @DisplayName("RN021 / RN022: Deve classificar como INSUFFICIENT_DATA quando não houver dados territoriais nem observações")
    void shouldClassifyAsInsufficientDataWhenNoInformationAvailable() {
        RiskAssessment assessment = engine.evaluate(
                municipality, null, EventCategory.HEAVY_RAIN, List.of(), List.of()
        );

        assertThat(assessment.getAssessmentStatus()).isEqualTo(AssessmentStatus.INSUFFICIENT_DATA);
        assertThat(assessment.getConfidenceLevel()).isEqualTo(ConfidenceLevel.LOW);
        assertThat(assessment.getFactors()).anyMatch(f -> "DATA_SUFFICIENCY".equals(f.getFactorType()));
    }

    @Test
    @DisplayName("RN020: A probabilidade percentual deve permanecer nula na ausência de modelo estatístico validado")
    void shouldKeepProbabilityPercentageNullWithoutStatisticalModel() {
        RiskAssessment assessment = engine.evaluate(
                municipality, null, EventCategory.FLASH_FLOOD, List.of(), List.of()
        );

        assertThat(assessment.getProbabilityPercentage()).isNull();
    }
}
