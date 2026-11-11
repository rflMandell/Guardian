package br.com.guardian.occurrence.api;

import br.com.guardian.occurrence.api.dto.CoordinatesDto;
import br.com.guardian.occurrence.api.dto.LinkOccurrenceRequest;
import br.com.guardian.occurrence.api.dto.OccurrenceDetailData;
import br.com.guardian.occurrence.api.dto.UpdateOccurrenceStatusRequest;
import br.com.guardian.occurrence.application.OccurrenceService;
import br.com.guardian.occurrence.domain.model.EventCategory;
import br.com.guardian.occurrence.domain.model.ReportStatus;
import br.com.guardian.shared.exception.BusinessException;
import br.com.guardian.shared.exception.GlobalExceptionHandler;
import br.com.guardian.shared.exception.ResourceNotFoundException;
import br.com.guardian.shared.security.CustomUserDetailsService;
import br.com.guardian.shared.security.JwtTokenProvider;
import br.com.guardian.shared.security.SecurityConfig;
import br.com.guardian.shared.security.UserPrincipal;
import br.com.guardian.usermanagement.domain.model.AccountType;
import br.com.guardian.usermanagement.domain.model.UserStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminOccurrenceController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("AdminOccurrenceController - Testes administrativos de gestão e vínculo de ocorrências")
class AdminOccurrenceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OccurrenceService occurrenceService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private UsernamePasswordAuthenticationToken createAuthToken(UUID userId, String... roles) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();

        UserPrincipal principal = new UserPrincipal(
                userId,
                "Operador Defesa Civil",
                "operador@guardian.gov.br",
                "hash",
                AccountType.ADMIN,
                UserStatus.ACTIVE,
                authorities
        );

        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    @Test
    @DisplayName("ADM-OCC-001: Deve permitir que operador municipal atualize status do relato para UNDER_ANALYSIS")
    void shouldAllowOperatorToUpdateStatus() throws Exception {
        UUID operatorId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();

        OccurrenceDetailData detail = new OccurrenceDetailData(
                reportId,
                EventCategory.FLOODING,
                "Equipe deslocada para o local.",
                new CoordinatesDto(-23.5505, -46.6333),
                ReportStatus.UNDER_ANALYSIS,
                true,
                munId,
                Instant.now(),
                Instant.now(),
                0
        );

        when(occurrenceService.updateStatus(eq(reportId), any(UpdateOccurrenceStatusRequest.class), eq(operatorId)))
                .thenReturn(detail);

        UpdateOccurrenceStatusRequest request = new UpdateOccurrenceStatusRequest(
                ReportStatus.UNDER_ANALYSIS,
                "Equipe deslocada para o local."
        );

        mockMvc.perform(patch("/api/v1/admin/occurrences/{occurrenceId}", reportId)
                        .with(authentication(createAuthToken(operatorId, "MUNICIPAL_OPERATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(reportId.toString()))
                .andExpect(jsonPath("$.data.status").value("UNDER_ANALYSIS"));
    }

    @Test
    @DisplayName("ADM-OCC-002: Deve permitir que operador vincule relato a incidente retornando 204 No Content")
    void shouldAllowOperatorToLinkReportToIncident() throws Exception {
        UUID operatorId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        doNothing().when(occurrenceService).linkToIncident(eq(reportId), eq(incidentId), eq(operatorId));

        LinkOccurrenceRequest request = new LinkOccurrenceRequest(incidentId);

        mockMvc.perform(post("/api/v1/admin/occurrences/{occurrenceId}/link", reportId)
                        .with(authentication(createAuthToken(operatorId, "MUNICIPAL_OPERATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("ADM-OCC-003: Deve recusar acesso a cidadão comum sem perfil de operador (403 Forbidden)")
    void shouldDenyAccessToCitizen() throws Exception {
        UUID citizenId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        LinkOccurrenceRequest request = new LinkOccurrenceRequest(incidentId);

        mockMvc.perform(post("/api/v1/admin/occurrences/{occurrenceId}/link", reportId)
                        .with(authentication(createAuthToken(citizenId, "CITIZEN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADM-OCC-004: Deve retornar 422 quando relato já estiver vinculado a um incidente")
    void shouldReturn422WhenReportAlreadyLinked() throws Exception {
        UUID operatorId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        doThrow(new BusinessException("REPORT_ALREADY_LINKED", "Este relato já se encontra vinculado a um incidente."))
                .when(occurrenceService).linkToIncident(eq(reportId), eq(incidentId), eq(operatorId));

        LinkOccurrenceRequest request = new LinkOccurrenceRequest(incidentId);

        mockMvc.perform(post("/api/v1/admin/occurrences/{occurrenceId}/link", reportId)
                        .with(authentication(createAuthToken(operatorId, "MUNICIPAL_OPERATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("REPORT_ALREADY_LINKED"));
    }

    @Test
    @DisplayName("ADM-OCC-005: Deve retornar 404 quando incidente a ser vinculado não for encontrado")
    void shouldReturn404WhenIncidentNotFound() throws Exception {
        UUID operatorId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        UUID nonExistentIncidentId = UUID.randomUUID();

        doThrow(new ResourceNotFoundException("Incidente", nonExistentIncidentId))
                .when(occurrenceService).linkToIncident(eq(reportId), eq(nonExistentIncidentId), eq(operatorId));

        LinkOccurrenceRequest request = new LinkOccurrenceRequest(nonExistentIncidentId);

        mockMvc.perform(post("/api/v1/admin/occurrences/{occurrenceId}/link", reportId)
                        .with(authentication(createAuthToken(operatorId, "MUNICIPAL_OPERATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
