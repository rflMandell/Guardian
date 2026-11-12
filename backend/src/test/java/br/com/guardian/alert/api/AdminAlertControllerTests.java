package br.com.guardian.alert.api;

import br.com.guardian.alert.api.dto.AlertResponseData;
import br.com.guardian.alert.api.dto.CloseAlertRequest;
import br.com.guardian.alert.api.dto.CreateAlertRequest;
import br.com.guardian.alert.api.dto.UpdateAlertRequest;
import br.com.guardian.alert.application.AlertService;
import br.com.guardian.alert.domain.model.AlertOriginType;
import br.com.guardian.alert.domain.model.AlertStatus;
import br.com.guardian.risk.domain.model.RiskLevel;
import br.com.guardian.shared.exception.AccessDeniedException;
import br.com.guardian.shared.exception.GlobalExceptionHandler;
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

@WebMvcTest(AdminAlertController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("AdminAlertController - Testes administrativos de emissão e gestão de alertas")
class AdminAlertControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertService alertService;

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
    @DisplayName("ADM-ALERT-001: Deve permitir que operador municipal emita alerta retornando 201 Created")
    void shouldAllowOperatorToCreateAlert() throws Exception {
        UUID operatorId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();

        AlertResponseData responseData = new AlertResponseData(
                alertId, munId, "Alerta de Alagamento", "Risco elevado na região.",
                RiskLevel.HIGH, AlertOriginType.GUARDIAN, AlertStatus.PUBLISHED,
                Instant.now(), Instant.now().plusSeconds(14400), "CURRENT",
                List.of(), List.of("Evite a região afetada.")
        );

        when(alertService.createAlert(any(CreateAlertRequest.class), eq(operatorId)))
                .thenReturn(responseData);

        CreateAlertRequest request = new CreateAlertRequest(
                munId, "Alerta de Alagamento", "Risco elevado na região.",
                RiskLevel.HIGH, AlertOriginType.GUARDIAN,
                Instant.now(), Instant.now().plusSeconds(14400),
                List.of("Evite a região afetada."), null, null, "Bacia Central"
        );

        mockMvc.perform(post("/api/v1/admin/alerts")
                        .with(authentication(createAuthToken(operatorId, "MUNICIPAL_OPERATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(alertId.toString()))
                .andExpect(jsonPath("$.data.title").value("Alerta de Alagamento"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("ADM-ALERT-002: Deve permitir que operador atualize o alerta retornando 200 OK")
    void shouldAllowOperatorToUpdateAlert() throws Exception {
        UUID operatorId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();

        AlertResponseData updatedData = new AlertResponseData(
                alertId, munId, "Alerta de Alagamento", "Situação agravada.",
                RiskLevel.CRITICAL, AlertOriginType.GUARDIAN, AlertStatus.UPDATED,
                Instant.now(), Instant.now().plusSeconds(7200), "CURRENT",
                List.of(), List.of("Afaste-se imediatamente.")
        );

        when(alertService.updateAlert(eq(alertId), any(UpdateAlertRequest.class), eq(operatorId)))
                .thenReturn(updatedData);

        UpdateAlertRequest request = new UpdateAlertRequest(
                RiskLevel.CRITICAL, "Situação agravada.",
                Instant.now().plusSeconds(7200), List.of("Afaste-se imediatamente."),
                "Elevação súbita do nível das águas."
        );

        mockMvc.perform(patch("/api/v1/admin/alerts/{alertId}", alertId)
                        .with(authentication(createAuthToken(operatorId, "MUNICIPAL_OPERATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(alertId.toString()))
                .andExpect(jsonPath("$.data.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.data.status").value("UPDATED"));
    }

    @Test
    @DisplayName("ADM-ALERT-003: Deve permitir que operador encerre o alerta retornando 204 No Content")
    void shouldAllowOperatorToCloseAlert() throws Exception {
        UUID operatorId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();

        doNothing().when(alertService).closeAlert(eq(alertId), any(CloseAlertRequest.class), eq(operatorId));

        CloseAlertRequest request = new CloseAlertRequest("Situação normalizada.");

        mockMvc.perform(post("/api/v1/admin/alerts/{alertId}/close", alertId)
                        .with(authentication(createAuthToken(operatorId, "MUNICIPAL_OPERATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("ADM-ALERT-004: Deve negar emissão de alerta para cidadão comum (403 Forbidden)")
    void shouldDenyCreateAlertForCitizen() throws Exception {
        UUID citizenId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();

        CreateAlertRequest request = new CreateAlertRequest(
                munId, "Alerta Fake", "Mensagem.", RiskLevel.HIGH,
                AlertOriginType.GUARDIAN, Instant.now(), Instant.now().plusSeconds(3600),
                List.of(), null, null, null
        );

        mockMvc.perform(post("/api/v1/admin/alerts")
                        .with(authentication(createAuthToken(citizenId, "CITIZEN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADM-ALERT-005: RN001 / RBAC-002: Deve negar emissão quando operador não tem escopo no município")
    void shouldDenyCreateAlertWhenOperatorLacksScope() throws Exception {
        UUID operatorId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();

        when(alertService.createAlert(any(CreateAlertRequest.class), eq(operatorId)))
                .thenThrow(new AccessDeniedException("Operador não possui escopo no município."));

        CreateAlertRequest request = new CreateAlertRequest(
                munId, "Alerta", "Mensagem.", RiskLevel.HIGH,
                AlertOriginType.GUARDIAN, Instant.now(), Instant.now().plusSeconds(3600),
                List.of(), null, null, null
        );

        mockMvc.perform(post("/api/v1/admin/alerts")
                        .with(authentication(createAuthToken(operatorId, "MUNICIPAL_OPERATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
