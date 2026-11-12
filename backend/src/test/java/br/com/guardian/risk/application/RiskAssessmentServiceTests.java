package br.com.guardian.risk.application;

import br.com.guardian.accesscontrol.application.MunicipalScopeService;
import br.com.guardian.geolocation.domain.model.RiskArea;
import br.com.guardian.geolocation.domain.repository.RiskAreaRepository;
import br.com.guardian.geolocation.util.GeometryUtils;
import br.com.guardian.municipality.domain.model.Municipality;
import br.com.guardian.municipality.domain.repository.MunicipalityRepository;
import br.com.guardian.occurrence.domain.model.EventCategory;
import br.com.guardian.occurrence.domain.repository.IncidentRepository;
import br.com.guardian.occurrence.domain.repository.OccurrenceReportRepository;
import br.com.guardian.risk.api.dto.RecalculateResponseData;
import br.com.guardian.risk.api.dto.RecalculateRiskRequest;
import br.com.guardian.risk.api.dto.RiskAssessmentResponseData;
import br.com.guardian.risk.domain.model.AssessmentStatus;
import br.com.guardian.risk.domain.model.ConfidenceLevel;
import br.com.guardian.risk.domain.model.RiskAssessment;
import br.com.guardian.risk.domain.model.RiskLevel;
import br.com.guardian.risk.domain.repository.RiskAssessmentRepository;
import br.com.guardian.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskAssessmentService - Testes unitários do serviço de avaliação de risco")
class RiskAssessmentServiceTests {

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    @Mock
    private RiskAreaRepository riskAreaRepository;

    @Mock
    private MunicipalityRepository municipalityRepository;

    @Mock
    private OccurrenceReportRepository reportRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private MunicipalScopeService municipalScopeService;

    @Mock
    private RiskAssessmentEngine riskAssessmentEngine;

    private RiskAssessmentService riskAssessmentService;

    @BeforeEach
    void setUp() {
        riskAssessmentService = new RiskAssessmentService(
                riskAssessmentRepository,
                riskAreaRepository,
                municipalityRepository,
                reportRepository,
                incidentRepository,
                municipalScopeService,
                riskAssessmentEngine
        );
    }

    @Test
    @DisplayName("RN019: Deve solicitar recálculo de risco com sucesso retornando status QUEUED")
    void shouldRecalculateRiskSuccessfully() {
        UUID munId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();
        Municipality municipality = new Municipality("Petrópolis", "RJ", "3303906", "BR");
        ReflectionTestUtils.setField(municipality, "id", munId);

        RiskAssessment assessment = new RiskAssessment(
                munId, null, EventCategory.HEAVY_RAIN,
                RiskLevel.HIGH, ConfidenceLevel.MEDIUM,
                AssessmentStatus.VALID, Instant.now(),
                Instant.now().plus(2, ChronoUnit.HOURS), "v1.0.0-rules"
        );
        ReflectionTestUtils.setField(assessment, "id", UUID.randomUUID());

        when(municipalityRepository.findById(munId)).thenReturn(Optional.of(municipality));
        when(reportRepository.findRecentReportsByMunicipalityAndType(eq(munId), any(), any())).thenReturn(List.of());
        when(incidentRepository.findByMunicipalityIdAndStatusNot(eq(munId), eq("CLOSED"))).thenReturn(List.of());
        when(riskAssessmentEngine.evaluate(any(), any(), any(), any(), any())).thenReturn(assessment);
        when(riskAssessmentRepository.save(any(RiskAssessment.class))).thenReturn(assessment);

        RecalculateRiskRequest request = new RecalculateRiskRequest(munId, null, EventCategory.HEAVY_RAIN);

        RecalculateResponseData response = riskAssessmentService.recalculateRisk(request, operatorId);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("QUEUED");
        assertThat(response.assessmentId()).isEqualTo(assessment.getId());
        verify(municipalScopeService).validateScope(operatorId, munId);
        verify(riskAssessmentRepository).save(any(RiskAssessment.class));
    }

    @Test
    @DisplayName("RN001: Deve rejeitar recálculo se a área de risco pertencer a outro município")
    void shouldRejectWhenRiskAreaBelongsToDifferentMunicipality() {
        UUID mun1 = UUID.randomUUID();
        UUID mun2 = UUID.randomUUID();
        UUID riskAreaId = UUID.randomUUID();

        Municipality m1 = new Municipality("Petrópolis", "RJ", "3303906", "BR");
        ReflectionTestUtils.setField(m1, "id", mun1);

        Municipality m2 = new Municipality("Teresópolis", "RJ", "3305802", "BR");
        ReflectionTestUtils.setField(m2, "id", mun2);

        RiskArea riskArea = new RiskArea(
                m2, null, "LANDSLIDE",
                GeometryUtils.createMultiPolygon(), "HIGH"
        );
        ReflectionTestUtils.setField(riskArea, "id", riskAreaId);

        when(municipalityRepository.findById(mun1)).thenReturn(Optional.of(m1));
        when(riskAreaRepository.findById(riskAreaId)).thenReturn(Optional.of(riskArea));

        RecalculateRiskRequest request = new RecalculateRiskRequest(mun1, riskAreaId, EventCategory.LANDSLIDE);

        assertThatThrownBy(() -> riskAssessmentService.recalculateRisk(request, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("A área de risco informada não pertence ao município selecionado.");
    }

    @Test
    @DisplayName("RN021: Deve marcar a avaliação como STALE_DATA quando validUntil estiver no passado")
    void shouldMarkAsStaleDataWhenExpired() {
        UUID munId = UUID.randomUUID();
        Instant evaluatedAt = Instant.now().minus(10, ChronoUnit.HOURS);
        Instant expiredValidUntil = Instant.now().minus(2, ChronoUnit.HOURS);

        RiskAssessment assessment = new RiskAssessment(
                munId, null, EventCategory.FLOODING,
                RiskLevel.MODERATE, ConfidenceLevel.MEDIUM,
                AssessmentStatus.VALID, evaluatedAt,
                expiredValidUntil, "v1.0.0-rules"
        );

        RiskAssessmentResponseData responseData = riskAssessmentService.toResponseData(assessment);

        assertThat(responseData.dataStatus()).isEqualTo(AssessmentStatus.STALE_DATA.name());
    }
}
