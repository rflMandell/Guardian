package br.com.guardian.municipality.api;

import br.com.guardian.municipality.api.dto.CreateMunicipalityRequest;
import br.com.guardian.municipality.api.dto.MunicipalityResponseData;
import br.com.guardian.municipality.api.dto.MunicipalityStatusResponseData;
import br.com.guardian.municipality.application.MunicipalityService;
import br.com.guardian.shared.exception.BusinessException;
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

@WebMvcTest(MunicipalityController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("MunicipalityController - Testes de API pública de municípios")
class MunicipalityControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MunicipalityService municipalityService;

    @Test
    @DisplayName("MUN-001: Deve criar município com sucesso retornando 201 Created")
    void shouldCreateMunicipalitySuccessfully() throws Exception {
        UUID munId = UUID.randomUUID();
        MunicipalityResponseData responseData = new MunicipalityResponseData(
                munId, "São Paulo", "SP", "BR", "3550308", "ACTIVE"
        );

        when(municipalityService.createMunicipality(any(CreateMunicipalityRequest.class)))
                .thenReturn(responseData);

        CreateMunicipalityRequest request = new CreateMunicipalityRequest(
                "São Paulo", "SP", "3550308", "BR"
        );

        mockMvc.perform(post("/api/v1/municipalities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(munId.toString()))
                .andExpect(jsonPath("$.data.name").value("São Paulo"))
                .andExpect(jsonPath("$.data.state").value("SP"))
                .andExpect(jsonPath("$.data.ibgeCode").value("3550308"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("MUN-002: Deve retornar 422 ao tentar cadastrar município com código IBGE duplicado")
    void shouldReturn422WhenIbgeCodeAlreadyExists() throws Exception {
        when(municipalityService.createMunicipality(any(CreateMunicipalityRequest.class)))
                .thenThrow(new BusinessException("IBGE_CODE_ALREADY_EXISTS", "Já existe um município cadastrado com o código IBGE '3550308'."));

        CreateMunicipalityRequest request = new CreateMunicipalityRequest(
                "São Paulo", "SP", "3550308", "BR"
        );

        mockMvc.perform(post("/api/v1/municipalities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("IBGE_CODE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Já existe um município cadastrado com o código IBGE '3550308'."));
    }

    @Test
    @DisplayName("MUN-003: Deve retornar 422 ao enviar requisição com campos obrigatórios inválidos")
    void shouldReturn422WhenRequestFieldsAreInvalid() throws Exception {
        CreateMunicipalityRequest request = new CreateMunicipalityRequest(
                "", "S", "123", "B"
        );

        mockMvc.perform(post("/api/v1/municipalities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("MUN-004: Deve listar municípios com paginação e suporte a filtros")
    void shouldListMunicipalitiesWithPagination() throws Exception {
        UUID munId = UUID.randomUUID();
        MunicipalityResponseData item = new MunicipalityResponseData(
                munId, "Campinas", "SP", "BR", "3509502", "ACTIVE"
        );

        PageImpl<MunicipalityResponseData> pageResult = new PageImpl<>(
                List.of(item),
                PageRequest.of(0, 20),
                1
        );

        when(municipalityService.listMunicipalities(eq("Campinas"), eq("SP"), any()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/municipalities")
                        .param("search", "Campinas")
                        .param("state", "SP")
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(munId.toString()))
                .andExpect(jsonPath("$.data[0].name").value("Campinas"))
                .andExpect(jsonPath("$.pagination.page").value(0))
                .andExpect(jsonPath("$.pagination.size").value(20))
                .andExpect(jsonPath("$.pagination.totalElements").value(1))
                .andExpect(jsonPath("$.pagination.totalPages").value(1));
    }

    @Test
    @DisplayName("MUN-005: Deve consultar município por ID com sucesso")
    void shouldGetMunicipalityById() throws Exception {
        UUID munId = UUID.randomUUID();
        MunicipalityResponseData item = new MunicipalityResponseData(
                munId, "Rio de Janeiro", "RJ", "BR", "3304557", "ACTIVE"
        );

        when(municipalityService.getMunicipality(munId)).thenReturn(item);

        mockMvc.perform(get("/api/v1/municipalities/{municipalityId}", munId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(munId.toString()))
                .andExpect(jsonPath("$.data.name").value("Rio de Janeiro"))
                .andExpect(jsonPath("$.data.state").value("RJ"));
    }

    @Test
    @DisplayName("MUN-006: Deve retornar 404 quando município consultado não existir")
    void shouldReturn404WhenMunicipalityNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(municipalityService.getMunicipality(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Município", nonExistentId));

        mockMvc.perform(get("/api/v1/municipalities/{municipalityId}", nonExistentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("MUN-007: Deve retornar a situação do município com sucesso")
    void shouldGetMunicipalityStatus() throws Exception {
        UUID munId = UUID.randomUUID();
        MunicipalityStatusResponseData statusData = new MunicipalityStatusResponseData(
                munId, "MODERATE", 2, "CURRENT", Instant.now()
        );

        when(municipalityService.getMunicipalityStatus(munId)).thenReturn(statusData);

        mockMvc.perform(get("/api/v1/municipalities/{municipalityId}/status", munId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.municipalityId").value(munId.toString()))
                .andExpect(jsonPath("$.data.overallRiskLevel").value("MODERATE"))
                .andExpect(jsonPath("$.data.activeAlerts").value(2));
    }
}
