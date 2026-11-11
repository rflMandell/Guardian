package br.com.guardian.occurrence.api;

import br.com.guardian.occurrence.api.dto.CoordinatesDto;
import br.com.guardian.occurrence.api.dto.CreateOccurrenceRequest;
import br.com.guardian.occurrence.api.dto.OccurrenceDetailData;
import br.com.guardian.occurrence.api.dto.OccurrenceResponseData;
import br.com.guardian.occurrence.api.dto.OccurrenceSummaryData;
import br.com.guardian.occurrence.application.OccurrenceService;
import br.com.guardian.occurrence.domain.model.EventCategory;
import br.com.guardian.occurrence.domain.model.ReportStatus;
import br.com.guardian.shared.exception.GlobalExceptionHandler;
import br.com.guardian.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OccurrenceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("OccurrenceController - Testes de API pública de ocorrências")
class OccurrenceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OccurrenceService occurrenceService;

    @Test
    @DisplayName("OCC-001: Deve registrar relato anônimo com sucesso retornando 201 Created e status RECEIVED")
    void shouldRegisterAnonymousOccurrence() throws Exception {
        UUID reportId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();
        Instant now = Instant.now();

        OccurrenceResponseData responseData = new OccurrenceResponseData(
                reportId,
                EventCategory.FLOODING,
                ReportStatus.RECEIVED,
                true,
                munId,
                now
        );

        when(occurrenceService.createReport(any(CreateOccurrenceRequest.class), any()))
                .thenReturn(responseData);

        CreateOccurrenceRequest request = new CreateOccurrenceRequest(
                EventCategory.FLOODING,
                "Rua apresenta acúmulo significativo de água.",
                new CoordinatesDto(-23.5505, -46.6333),
                now,
                true
        );

        mockMvc.perform(post("/api/v1/occurrences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(reportId.toString()))
                .andExpect(jsonPath("$.data.eventCategory").value("FLOODING"))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                .andExpect(jsonPath("$.data.anonymous").value(true))
                .andExpect(jsonPath("$.data.municipalityId").value(munId.toString()))
                .andExpect(jsonPath("$.data.userId").doesNotExist());
    }

    @Test
    @DisplayName("OCC-002: Deve registrar relato autenticado preservando privacidade na resposta pública")
    void shouldRegisterAuthenticatedOccurrenceWithoutExposingUserId() throws Exception {
        UUID reportId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();
        Instant now = Instant.now();

        OccurrenceResponseData responseData = new OccurrenceResponseData(
                reportId,
                EventCategory.LANDSLIDE,
                ReportStatus.RECEIVED,
                false,
                munId,
                now
        );

        when(occurrenceService.createReport(any(CreateOccurrenceRequest.class), any()))
                .thenReturn(responseData);

        CreateOccurrenceRequest request = new CreateOccurrenceRequest(
                EventCategory.LANDSLIDE,
                "Encosta cedendo",
                new CoordinatesDto(-23.5505, -46.6333),
                now,
                false
        );

        mockMvc.perform(post("/api/v1/occurrences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(reportId.toString()))
                .andExpect(jsonPath("$.data.anonymous").value(false))
                .andExpect(jsonPath("$.data.userId").doesNotExist());
    }

    @Test
    @DisplayName("OCC-003: Deve retornar 422 quando dados obrigatórios forem omitidos")
    void shouldReturn422WhenRequiredFieldsMissing() throws Exception {
        CreateOccurrenceRequest request = new CreateOccurrenceRequest(
                null,
                "Descrição sem categoria",
                null,
                null,
                true
        );

        mockMvc.perform(post("/api/v1/occurrences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("OCC-004: Deve listar ocorrências com paginação preservando privacidade")
    void shouldListOccurrencesWithPagination() throws Exception {
        UUID reportId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();

        OccurrenceSummaryData item = new OccurrenceSummaryData(
                reportId,
                EventCategory.FLOODING,
                ReportStatus.RECEIVED,
                true,
                munId,
                -23.5505,
                -46.6333,
                Instant.now()
        );

        PageImpl<OccurrenceSummaryData> pageResult = new PageImpl<>(
                List.of(item),
                PageRequest.of(0, 20),
                1
        );

        when(occurrenceService.listReports(eq(munId), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/occurrences")
                        .param("municipalityId", munId.toString())
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(reportId.toString()))
                .andExpect(jsonPath("$.data[0].eventCategory").value("FLOODING"))
                .andExpect(jsonPath("$.data[0].userId").doesNotExist())
                .andExpect(jsonPath("$.pagination.page").value(0))
                .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    @DisplayName("OCC-005: Deve consultar ocorrência por ID com sucesso")
    void shouldGetOccurrenceById() throws Exception {
        UUID reportId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();

        OccurrenceDetailData detail = new OccurrenceDetailData(
                reportId,
                EventCategory.FLASH_FLOOD,
                "Enxurrada invadindo residências",
                new CoordinatesDto(-23.5505, -46.6333),
                ReportStatus.RECEIVED,
                true,
                munId,
                Instant.now(),
                Instant.now(),
                0
        );

        when(occurrenceService.getReport(reportId)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/occurrences/{occurrenceId}", reportId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(reportId.toString()))
                .andExpect(jsonPath("$.data.eventCategory").value("FLASH_FLOOD"))
                .andExpect(jsonPath("$.data.description").value("Enxurrada invadindo residências"))
                .andExpect(jsonPath("$.data.location.latitude").value(-23.5505))
                .andExpect(jsonPath("$.data.userId").doesNotExist());
    }

    @Test
    @DisplayName("OCC-006: Deve retornar 404 quando relato consultado não for encontrado")
    void shouldReturn404WhenReportNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        when(occurrenceService.getReport(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Relato de ocorrência", nonExistentId));

        mockMvc.perform(get("/api/v1/occurrences/{occurrenceId}", nonExistentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
