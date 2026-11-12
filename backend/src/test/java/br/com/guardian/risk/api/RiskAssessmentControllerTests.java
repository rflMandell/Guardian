package br.com.guardian.risk.api;

import br.com.guardian.occurrence.domain.model.EventCategory;
import br.com.guardian.risk.api.dto.RiskAssessmentResponseData;
import br.com.guardian.risk.api.dto.RiskFactorData;
import br.com.guardian.risk.application.RiskAssessmentService;
import br.com.guardian.risk.domain.model.ConfidenceLevel;
import br.com.guardian.risk.domain.model.RiskLevel;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RiskAssessmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("RiskAssessmentController - Testes de API pública de avaliações de risco")
class RiskAssessmentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RiskAssessmentService riskAssessmentService;

    @Test
    @DisplayName("RISK-001: Deve listar avaliações de risco com paginação e filtros")
    void shouldListRiskAssessmentsWithPagination() throws Exception {
        UUID assessmentId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();

        RiskFactorData factor = new RiskFactorData(
                UUID.randomUUID(), "CITIZEN_REPORTS", "Relatos de cidadãos",
                "INCREASES_RISK", "3 relatos nas últimas 4 horas", new BigDecimal("0.70")
        );

        RiskAssessmentResponseData item = new RiskAssessmentResponseData(
                assessmentId, munId, null,
                RiskLevel.HIGH, EventCategory.FLOODING,
                ConfidenceLevel.MEDIUM, null, "VALID",
                List.of(factor), Instant.now(), Instant.now().plusSeconds(7200)
        );

        PageImpl<RiskAssessmentResponseData> pageResult = new PageImpl<>(
                List.of(item),
                PageRequest.of(0, 20),
                1
        );

        when(riskAssessmentService.listAssessments(eq(munId), any(), any(), any(), any(), any(), any()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/risk-assessments")
                        .param("municipalityId", munId.toString())
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(assessmentId.toString()))
                .andExpect(jsonPath("$.data[0].riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.data[0].eventCategory").value("FLOODING"))
                .andExpect(jsonPath("$.data[0].factors[0].factorType").value("CITIZEN_REPORTS"))
                .andExpect(jsonPath("$.pagination.page").value(0))
                .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    @DisplayName("RISK-002: Deve consultar avaliação de risco por ID com sucesso")
    void shouldGetRiskAssessmentById() throws Exception {
        UUID assessmentId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();

        RiskAssessmentResponseData item = new RiskAssessmentResponseData(
                assessmentId, munId, null,
                RiskLevel.CRITICAL, EventCategory.LANDSLIDE,
                ConfidenceLevel.HIGH, null, "VALID",
                List.of(), Instant.now(), Instant.now().plusSeconds(3600)
        );

        when(riskAssessmentService.getAssessment(assessmentId)).thenReturn(item);

        mockMvc.perform(get("/api/v1/risk-assessments/{assessmentId}", assessmentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(assessmentId.toString()))
                .andExpect(jsonPath("$.data.riskLevel").value("CRITICAL"))
                .andExpect(jsonPath("$.data.eventCategory").value("LANDSLIDE"));
    }

    @Test
    @DisplayName("RISK-003: Deve retornar 404 quando avaliação consultada não for encontrada")
    void shouldReturn404WhenAssessmentNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        when(riskAssessmentService.getAssessment(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Avaliação de Risco", nonExistentId));

        mockMvc.perform(get("/api/v1/risk-assessments/{assessmentId}", nonExistentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("RISK-004: Deve consultar a avaliação de risco mais recente")
    void shouldGetLatestRiskAssessment() throws Exception {
        UUID munId = UUID.randomUUID();
        RiskAssessmentResponseData item = new RiskAssessmentResponseData(
                UUID.randomUUID(), munId, null,
                RiskLevel.MODERATE, EventCategory.HEAVY_RAIN,
                ConfidenceLevel.MEDIUM, null, "VALID",
                List.of(), Instant.now(), Instant.now().plusSeconds(14400)
        );

        when(riskAssessmentService.getLatestAssessment(munId, EventCategory.HEAVY_RAIN))
                .thenReturn(item);

        mockMvc.perform(get("/api/v1/risk-assessments/latest")
                        .param("municipalityId", munId.toString())
                        .param("eventCategory", "HEAVY_RAIN")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.riskLevel").value("MODERATE"))
                .andExpect(jsonPath("$.data.eventCategory").value("HEAVY_RAIN"));
    }
}
