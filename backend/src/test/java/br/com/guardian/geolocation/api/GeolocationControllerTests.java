package br.com.guardian.geolocation.api;

import br.com.guardian.geolocation.api.dto.*;
import br.com.guardian.geolocation.application.GeolocationService;
import br.com.guardian.shared.exception.GlobalExceptionHandler;
import br.com.guardian.shared.exception.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GeolocationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("GeolocationController - Testes de resolução geográfica e situação territorial")
class GeolocationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GeolocationService geolocationService;

    @Test
    @DisplayName("GEO-001: Deve resolver localização por coordenadas GPS com sucesso")
    void shouldResolveLocationByCoordinates() throws Exception {
        UUID munId = UUID.randomUUID();
        LocationResolvedData responseData = new LocationResolvedData(
                munId, "São Paulo", "SP", -23.5505, -46.6333,
                "São Paulo, SP - Lat: -23.5505, Lon: -46.6333", "GPS"
        );

        when(geolocationService.resolveLocation(any(ResolveLocationRequest.class)))
                .thenReturn(responseData);

        ResolveLocationRequest request = new ResolveLocationRequest(-23.5505, -46.6333, null, null);

        mockMvc.perform(post("/api/v1/geolocation/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.municipalityId").value(munId.toString()))
                .andExpect(jsonPath("$.data.municipalityName").value("São Paulo"))
                .andExpect(jsonPath("$.data.state").value("SP"))
                .andExpect(jsonPath("$.data.latitude").value(-23.5505))
                .andExpect(jsonPath("$.data.longitude").value(-46.6333))
                .andExpect(jsonPath("$.data.resolutionSource").value("GPS"));
    }

    @Test
    @DisplayName("GEO-002: Deve resolver localização por CEP com sucesso")
    void shouldResolveLocationByPostalCode() throws Exception {
        LocationResolvedData responseData = new LocationResolvedData(
                null, "Localidade por CEP", "SP", -23.5505, -46.6333,
                "CEP 01310100, Brasil", "POSTAL_CODE"
        );

        when(geolocationService.resolveLocation(any(ResolveLocationRequest.class)))
                .thenReturn(responseData);

        ResolveLocationRequest request = new ResolveLocationRequest(null, null, "01310-100", "BR");

        mockMvc.perform(post("/api/v1/geolocation/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resolutionSource").value("POSTAL_CODE"))
                .andExpect(jsonPath("$.data.state").value("SP"));
    }

    @Test
    @DisplayName("GEO-003: Deve retornar 422 ao enviar requisição de resolução sem parâmetros ou inválida")
    void shouldReturn422WhenResolveHasInvalidParams() throws Exception {
        when(geolocationService.resolveLocation(any(ResolveLocationRequest.class)))
                .thenThrow(new ValidationException("É necessário informar coordenadas (latitude e longitude) ou código postal (CEP)."));

        ResolveLocationRequest request = new ResolveLocationRequest(null, null, null, null);

        mockMvc.perform(post("/api/v1/geolocation/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("É necessário informar coordenadas (latitude e longitude) ou código postal (CEP)."));
    }

    @Test
    @DisplayName("GEO-004: Deve consultar a situação de risco por coordenadas geográficas")
    void shouldGetCoordinateRiskStatus() throws Exception {
        UUID munId = UUID.randomUUID();
        CoordinateStatusData statusData = new CoordinateStatusData(
                new CoordinateStatusData.MunicipalitySummary(munId, "São Paulo"),
                "MODERATE",
                "CURRENT",
                1,
                List.of("FLOOD", "LANDSLIDE"),
                Instant.now()
        );

        when(geolocationService.getCoordinateStatus(-23.5505, -46.6333))
                .thenReturn(statusData);

        mockMvc.perform(get("/api/v1/geolocation/status")
                        .param("latitude", "-23.5505")
                        .param("longitude", "-46.6333")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.municipality.id").value(munId.toString()))
                .andExpect(jsonPath("$.data.municipality.name").value("São Paulo"))
                .andExpect(jsonPath("$.data.riskLevel").value("MODERATE"))
                .andExpect(jsonPath("$.data.activeAlerts").value(1))
                .andExpect(jsonPath("$.data.riskFactors[0]").value("FLOOD"));
    }

    @Test
    @DisplayName("GEO-005: Deve executar geocodificação reversa")
    void shouldReverseGeocode() throws Exception {
        ReverseGeocodeData reverseData = new ReverseGeocodeData(
                "Avenida Paulista, 1000",
                "São Paulo",
                "SP",
                "01310-100",
                "BR"
        );

        when(geolocationService.reverseGeocode(-23.5505, -46.6333))
                .thenReturn(reverseData);

        mockMvc.perform(get("/api/v1/geolocation/reverse-geocode")
                        .param("latitude", "-23.5505")
                        .param("longitude", "-46.6333")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address").value("Avenida Paulista, 1000"))
                .andExpect(jsonPath("$.data.city").value("São Paulo"))
                .andExpect(jsonPath("$.data.state").value("SP"))
                .andExpect(jsonPath("$.data.postalCode").value("01310-100"));
    }

    @Test
    @DisplayName("GEO-006: Deve executar geocodificação direta a partir de CEP ou endereço")
    void shouldGeocode() throws Exception {
        GeocodeData geocodeData = new GeocodeData(
                -23.5617,
                -46.6559,
                "Avenida Paulista, São Paulo - SP"
        );

        when(geolocationService.geocode("01310-100", null, null, null))
                .thenReturn(geocodeData);

        mockMvc.perform(get("/api/v1/geolocation/geocode")
                        .param("postalCode", "01310-100")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latitude").value(-23.5617))
                .andExpect(jsonPath("$.data.longitude").value(-46.6559))
                .andExpect(jsonPath("$.data.formattedAddress").value("Avenida Paulista, São Paulo - SP"));
    }
}
