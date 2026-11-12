package br.com.guardian.alert.api;

import br.com.guardian.alert.api.dto.AlertAreaDto;
import br.com.guardian.alert.api.dto.AlertResponseData;
import br.com.guardian.alert.application.AlertService;
import br.com.guardian.alert.domain.model.AlertOriginType;
import br.com.guardian.alert.domain.model.AlertStatus;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("AlertController - Testes de API pública de alertas")
class AlertControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertService alertService;

    @Test
    @DisplayName("ALERT-001: Deve listar alertas ativos com paginação e filtros")
    void shouldListActiveAlertsWithPagination() throws Exception {
        UUID alertId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();

        AlertResponseData item = new AlertResponseData(
                alertId, munId, "Alerta de Inundação", "Possibilidade de alagamento.",
                RiskLevel.HIGH, AlertOriginType.GUARDIAN, AlertStatus.PUBLISHED,
                Instant.now(), Instant.now().plusSeconds(14400), "CURRENT",
                List.of(new AlertAreaDto(UUID.randomUUID(), "Bacia Central")),
                List.of("Evite a região.")
        );

        PageImpl<AlertResponseData> pageResult = new PageImpl<>(
                List.of(item),
                PageRequest.of(0, 20),
                1
        );

        when(alertService.listAlerts(eq(munId), any(), any(), any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/alerts")
                        .param("municipalityId", munId.toString())
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(alertId.toString()))
                .andExpect(jsonPath("$.data[0].title").value("Alerta de Inundação"))
                .andExpect(jsonPath("$.data[0].severity").value("HIGH"))
                .andExpect(jsonPath("$.pagination.page").value(0))
                .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    @DisplayName("ALERT-002: Deve consultar alerta por ID com sucesso")
    void shouldGetAlertById() throws Exception {
        UUID alertId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();

        AlertResponseData item = new AlertResponseData(
                alertId, munId, "Alerta Crítico de Deslizamento", "Evacuação imediata necessária.",
                RiskLevel.CRITICAL, AlertOriginType.GUARDIAN, AlertStatus.PUBLISHED,
                Instant.now(), Instant.now().plusSeconds(7200), "CURRENT",
                List.of(), List.of("Dirija-se ao abrigo mais próximo.")
        );

        when(alertService.getAlert(alertId)).thenReturn(item);

        mockMvc.perform(get("/api/v1/alerts/{alertId}", alertId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(alertId.toString()))
                .andExpect(jsonPath("$.data.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.data.title").value("Alerta Crítico de Deslizamento"));
    }

    @Test
    @DisplayName("ALERT-003: Deve retornar 404 quando alerta não for encontrado")
    void shouldReturn404WhenAlertNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        when(alertService.getAlert(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Alerta", nonExistentId));

        mockMvc.perform(get("/api/v1/alerts/{alertId}", nonExistentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
