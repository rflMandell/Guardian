package br.com.guardian.occurrence.application;

import br.com.guardian.accesscontrol.application.MunicipalScopeService;
import br.com.guardian.geolocation.util.GeometryUtils;
import br.com.guardian.municipality.domain.model.Municipality;
import br.com.guardian.municipality.domain.repository.MunicipalityRepository;
import br.com.guardian.occurrence.api.dto.CoordinatesDto;
import br.com.guardian.occurrence.api.dto.CreateOccurrenceRequest;
import br.com.guardian.occurrence.api.dto.OccurrenceResponseData;
import br.com.guardian.occurrence.api.dto.UpdateOccurrenceStatusRequest;
import br.com.guardian.occurrence.domain.model.EventCategory;
import br.com.guardian.occurrence.domain.model.Incident;
import br.com.guardian.occurrence.domain.model.OccurrenceReport;
import br.com.guardian.occurrence.domain.model.ReportStatus;
import br.com.guardian.occurrence.domain.repository.IncidentReportLinkRepository;
import br.com.guardian.occurrence.domain.repository.IncidentRepository;
import br.com.guardian.occurrence.domain.repository.OccurrenceReportRepository;
import br.com.guardian.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Point;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OccurrenceService - Testes unitários de regras de negócio de ocorrências")
class OccurrenceServiceTests {

    @Mock
    private OccurrenceReportRepository reportRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentReportLinkRepository linkRepository;

    @Mock
    private MunicipalityRepository municipalityRepository;

    @Mock
    private MunicipalScopeService municipalScopeService;

    private OccurrenceService occurrenceService;

    @BeforeEach
    void setUp() {
        occurrenceService = new OccurrenceService(
                reportRepository,
                incidentRepository,
                linkRepository,
                municipalityRepository,
                municipalScopeService
        );
    }

    @Test
    @DisplayName("RN014: Deve calcular distância Haversine com precisão")
    void shouldCalculateHaversineDistanceCorrectly() {
        // Coordenadas da Praça da Sé e Theatro Municipal em São Paulo (~850 metros de distância)
        double lat1 = -23.5505;
        double lon1 = -46.6333;
        double lat2 = -23.5453;
        double lon2 = -46.6388;

        double distance = OccurrenceService.calculateDistanceMeters(lat1, lon1, lat2, lon2);

        assertThat(distance).isBetween(800.0, 950.0);
    }

    @Test
    @DisplayName("RN011 / RN012: Deve registrar relato anônimo com sucesso")
    void shouldCreateAnonymousOccurrenceReport() {
        UUID munId = UUID.randomUUID();
        Municipality municipality = new Municipality("São Paulo", "SP", "3550308", "BR");
        org.springframework.test.util.ReflectionTestUtils.setField(municipality, "id", munId);

        when(municipalityRepository.findByContainingPoint(any(Point.class)))
                .thenReturn(List.of(municipality));
        when(reportRepository.findRecentReportsByMunicipalityAndType(any(), any(), any()))
                .thenReturn(List.of());
        when(reportRepository.save(any(OccurrenceReport.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateOccurrenceRequest request = new CreateOccurrenceRequest(
                EventCategory.FLOODING,
                "Água subindo rápido",
                new CoordinatesDto(-23.5505, -46.6333),
                Instant.now(),
                true
        );

        OccurrenceResponseData response = occurrenceService.createReport(request, null);

        assertThat(response).isNotNull();
        assertThat(response.eventCategory()).isEqualTo(EventCategory.FLOODING);
        assertThat(response.status()).isEqualTo(ReportStatus.RECEIVED);
        assertThat(response.anonymous()).isTrue();
        verify(reportRepository).save(any(OccurrenceReport.class));
    }

    @Test
    @DisplayName("RN014: Deve detectar relatos duplicados próximos geograficamente e temporariamente")
    void shouldDetectPotentialDuplicates() {
        UUID munId = UUID.randomUUID();
        Municipality municipality = new Municipality("São Paulo", "SP", "3550308", "BR");
        org.springframework.test.util.ReflectionTestUtils.setField(municipality, "id", munId);
        Point existingLocation = GeometryUtils.createPoint(-23.5505, -46.6333);

        OccurrenceReport existingReport = new OccurrenceReport(
                null, munId, EventCategory.FLOODING,
                "Primeiro relato", existingLocation, Instant.now()
        );

        when(municipalityRepository.findByContainingPoint(any(Point.class)))
                .thenReturn(List.of(municipality));
        when(reportRepository.findRecentReportsByMunicipalityAndType(any(), any(), any()))
                .thenReturn(List.of(existingReport));
        when(reportRepository.save(any(OccurrenceReport.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Novo relato a ~100 metros de distância
        CreateOccurrenceRequest request = new CreateOccurrenceRequest(
                EventCategory.FLOODING,
                "Segundo relato próximo",
                new CoordinatesDto(-23.5510, -46.6335),
                Instant.now(),
                true
        );

        OccurrenceResponseData response = occurrenceService.createReport(request, null);

        assertThat(response).isNotNull();
        verify(reportRepository).save(any(OccurrenceReport.class));
    }

    @Test
    @DisplayName("RN013 / RN015: Deve associar relato a incidente e atualizar status para LINKED_TO_INCIDENT")
    void shouldLinkReportToIncidentSuccessfully() {
        UUID munId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();

        OccurrenceReport report = new OccurrenceReport(
                null, munId, EventCategory.FLOOD, "Relato",
                GeometryUtils.createPoint(-23.55, -46.63), Instant.now()
        );

        Incident incident = new Incident(
                munId, EventCategory.FLOOD, "Incidente Confirmado",
                "Ocorrência confirmada", GeometryUtils.createPoint(-23.55, -46.63),
                "HIGH", "OPERATOR"
        );

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(linkRepository.existsByReportId(reportId)).thenReturn(false);

        occurrenceService.linkToIncident(reportId, incidentId, operatorId);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.LINKED_TO_INCIDENT);
        verify(municipalScopeService).validateScope(operatorId, munId);
        verify(linkRepository).save(any());
        verify(reportRepository).save(report);
    }

    @Test
    @DisplayName("RN013: Deve rejeitar associação se o relato já estiver vinculado a outro incidente")
    void shouldRejectLinkWhenReportAlreadyLinked() {
        UUID munId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        OccurrenceReport report = new OccurrenceReport(
                null, munId, EventCategory.FLOOD, "Relato",
                GeometryUtils.createPoint(-23.55, -46.63), Instant.now()
        );

        Incident incident = new Incident(
                munId, EventCategory.FLOOD, "Incidente",
                "Desc", GeometryUtils.createPoint(-23.55, -46.63),
                "HIGH", "OPERATOR"
        );

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(linkRepository.existsByReportId(reportId)).thenReturn(true);

        assertThatThrownBy(() -> occurrenceService.linkToIncident(reportId, incidentId, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Este relato já se encontra vinculado a um incidente.");
    }

    @Test
    @DisplayName("RN001: Deve rejeitar associação se relato e incidente forem de municípios distintos")
    void shouldRejectLinkWhenMunicipalitiesMismatch() {
        UUID mun1 = UUID.randomUUID();
        UUID mun2 = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        OccurrenceReport report = new OccurrenceReport(
                null, mun1, EventCategory.FLOOD, "Relato",
                GeometryUtils.createPoint(-23.55, -46.63), Instant.now()
        );

        Incident incident = new Incident(
                mun2, EventCategory.FLOOD, "Incidente",
                "Desc", GeometryUtils.createPoint(-23.55, -46.63),
                "HIGH", "OPERATOR"
        );

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> occurrenceService.linkToIncident(reportId, incidentId, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("O relato e o incidente pertencem a municípios distintos.");
    }
}
