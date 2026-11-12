package br.com.guardian.risk.api;

import br.com.guardian.occurrence.domain.model.EventCategory;
import br.com.guardian.risk.api.dto.RecalculateResponseData;
import br.com.guardian.risk.api.dto.RecalculateRiskRequest;
import br.com.guardian.risk.application.RiskAssessmentService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminRiskAssessmentController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("AdminRiskAssessmentController - Testes administrativos de cálculo e recálculo de risco")
class AdminRiskAssessmentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RiskAssessmentService riskAssessmentService;

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
    @DisplayName("ADM-RISK-001: Deve permitir que operador municipal solicite recálculo de risco retornando 202 Accepted")
    void shouldAllowOperatorToRecalculateRisk() throws Exception {
        UUID operatorId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();
        UUID assessmentId = UUID.randomUUID();

        RecalculateResponseData responseData = new RecalculateResponseData(
                "QUEUED", "Avaliação solicitada para processamento.", assessmentId
        );

        when(riskAssessmentService.recalculateRisk(any(RecalculateRiskRequest.class), eq(operatorId)))
                .thenReturn(responseData);

        RecalculateRiskRequest request = new RecalculateRiskRequest(munId, null, EventCategory.FLOODING);

        mockMvc.perform(post("/api/v1/admin/risk-assessments/recalculate")
                        .with(authentication(createAuthToken(operatorId, "MUNICIPAL_OPERATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.assessmentId").value(assessmentId.toString()));
    }

    @Test
    @DisplayName("ADM-RISK-002: Deve negar recálculo para cidadão comum sem perfil de operador (403 Forbidden)")
    void shouldDenyRecalculateForCitizen() throws Exception {
        UUID citizenId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();

        RecalculateRiskRequest request = new RecalculateRiskRequest(munId, null, EventCategory.FLOODING);

        mockMvc.perform(post("/api/v1/admin/risk-assessments/recalculate")
                        .with(authentication(createAuthToken(citizenId, "CITIZEN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADM-RISK-003: RN001 / RBAC-002: Deve negar acesso quando operador tenta recalcular em município fora do seu escopo")
    void shouldDenyAccessWhenOperatorLacksScope() throws Exception {
        UUID operatorId = UUID.randomUUID();
        UUID otherMunId = UUID.randomUUID();

        when(riskAssessmentService.recalculateRisk(any(RecalculateRiskRequest.class), eq(operatorId)))
                .thenThrow(new AccessDeniedException("Operador não possui escopo no município informado."));

        RecalculateRiskRequest request = new RecalculateRiskRequest(otherMunId, null, EventCategory.LANDSLIDE);

        mockMvc.perform(post("/api/v1/admin/risk-assessments/recalculate")
                        .with(authentication(createAuthToken(operatorId, "MUNICIPAL_OPERATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("ADM-RISK-004: Deve retornar 422 quando requisição contiver campos obrigatórios nulos")
    void shouldReturn422WhenFieldsAreInvalid() throws Exception {
        UUID operatorId = UUID.randomUUID();
        RecalculateRiskRequest request = new RecalculateRiskRequest(null, null, null);

        mockMvc.perform(post("/api/v1/admin/risk-assessments/recalculate")
                        .with(authentication(createAuthToken(operatorId, "MUNICIPAL_OPERATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details").isArray());
    }
}
