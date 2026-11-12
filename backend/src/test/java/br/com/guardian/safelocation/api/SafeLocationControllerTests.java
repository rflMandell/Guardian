package br.com.guardian.safelocation.api;

import br.com.guardian.occurrence.api.dto.CoordinatesDto;
import br.com.guardian.safelocation.api.dto.SafeLocationResponseData;
import br.com.guardian.safelocation.application.SafeLocationService;
import br.com.guardian.safelocation.domain.model.SafeLocationAvailability;
import br.com.guardian.safelocation.domain.model.SafeLocationOperationalStatus;
import br.com.guardian.safelocation.domain.model.SafeLocationType;
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

@WebMvcTest(SafeLocationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("SafeLocationController - Testes de API pública de pontos seguros")
class SafeLocationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SafeLocationService safeLocationService;

    @Test
    @DisplayName("SAFE-001: Deve listar pontos seguros com paginação e filtros")
    void shouldListSafeLocationsWithPagination() throws Exception {
        UUID locId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();

        SafeLocationResponseData item = new SafeLocationResponseData(
                locId, munId, "Abrigo Municipal Central", "Abrigo de acolhimento",
                SafeLocationType.SHELTER, "Rua do Acolhimento, 100",
                new CoordinatesDto(-22.505, -43.18), 200, 80,
                SafeLocationAvailability.AVAILABLE, true,
                SafeLocationOperationalStatus.OPEN, Instant.now(), "CURRENT"
        );

        PageImpl<SafeLocationResponseData> pageResult = new PageImpl<>(
                List.of(item),
                PageRequest.of(0, 20),
                1
        );

        when(safeLocationService.listSafeLocations(eq(munId), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/safe-locations")
                        .param("municipalityId", munId.toString())
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(locId.toString()))
                .andExpect(jsonPath("$.data[0].name").value("Abrigo Municipal Central"))
                .andExpect(jsonPath("$.data[0].availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.data[0].capacity").value(200))
                .andExpect(jsonPath("$.data[0].occupancy").value(80))
                .andExpect(jsonPath("$.pagination.page").value(0))
                .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    @DisplayName("SAFE-002: Deve consultar ponto seguro por ID com sucesso")
    void shouldGetSafeLocationById() throws Exception {
        UUID locId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();

        SafeLocationResponseData item = new SafeLocationResponseData(
                locId, munId, "Ginásio Esportivo", "Ginásio adaptado",
                SafeLocationType.GYMNASIUM, "Av. Brasil, 500",
                new CoordinatesDto(-22.505, -43.18), 300, 0,
                SafeLocationAvailability.AVAILABLE, true,
                SafeLocationOperationalStatus.OPEN, Instant.now(), "CURRENT"
        );

        when(safeLocationService.getSafeLocation(locId)).thenReturn(item);

        mockMvc.perform(get("/api/v1/safe-locations/{safeLocationId}", locId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(locId.toString()))
                .andExpect(jsonPath("$.data.name").value("Ginásio Esportivo"))
                .andExpect(jsonPath("$.data.type").value("GYMNASIUM"));
    }

    @Test
    @DisplayName("SAFE-003: Deve retornar 404 quando ponto seguro não for encontrado")
    void shouldReturn404WhenSafeLocationNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        when(safeLocationService.getSafeLocation(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Ponto Seguro", nonExistentId));

        mockMvc.perform(get("/api/v1/safe-locations/{safeLocationId}", nonExistentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
