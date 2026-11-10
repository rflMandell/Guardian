package br.com.guardian.municipality.api;

import br.com.guardian.accesscontrol.domain.model.MembershipStatus;
import br.com.guardian.municipality.api.dto.AssignMemberRequest;
import br.com.guardian.municipality.api.dto.CreateMunicipalityRequest;
import br.com.guardian.municipality.api.dto.MunicipalityMemberData;
import br.com.guardian.municipality.api.dto.MunicipalityResponseData;
import br.com.guardian.municipality.application.MunicipalityService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMunicipalityController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("AdminMunicipalityController - Testes administrativos e isolamento municipal")
class AdminMunicipalityControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MunicipalityService municipalityService;

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
                "Operador Teste",
                "operador@guardian.gov.br",
                "hash",
                AccountType.ADMIN,
                UserStatus.ACTIVE,
                authorities
        );

        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    @Test
    @DisplayName("ADM-001: Deve permitir que GLOBAL_ADMIN cadastre município via rota administrativa")
    void shouldAllowGlobalAdminToCreateMunicipality() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();
        MunicipalityResponseData responseData = new MunicipalityResponseData(
                munId, "Niterói", "RJ", "BR", "3303302", "ACTIVE"
        );

        when(municipalityService.createMunicipality(any(CreateMunicipalityRequest.class)))
                .thenReturn(responseData);

        CreateMunicipalityRequest request = new CreateMunicipalityRequest(
                "Niterói", "RJ", "3303302", "BR"
        );

        mockMvc.perform(post("/api/v1/admin/municipalities")
                        .with(authentication(createAuthToken(adminId, "GLOBAL_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(munId.toString()))
                .andExpect(jsonPath("$.data.name").value("Niterói"));
    }

    @Test
    @DisplayName("ADM-002: Deve negar criação de município para usuário sem role apropriada (403 Forbidden)")
    void shouldDenyCreateMunicipalityForUnauthorizedUser() throws Exception {
        UUID citizenId = UUID.randomUUID();
        CreateMunicipalityRequest request = new CreateMunicipalityRequest(
                "Niterói", "RJ", "3303302", "BR"
        );

        mockMvc.perform(post("/api/v1/admin/municipalities")
                        .with(authentication(createAuthToken(citizenId, "CITIZEN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADM-003: Deve vincular operador ao município retornando 201 Created")
    void shouldAssignOperatorToMunicipality() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        MunicipalityMemberData memberData = new MunicipalityMemberData(
                memberId, operatorId, "Operador Defesa Civil", "defesa@niteroi.rj.gov.br",
                munId, "ACTIVE", Instant.now()
        );

        when(municipalityService.assignMember(eq(munId), any(AssignMemberRequest.class)))
                .thenReturn(memberData);

        AssignMemberRequest request = new AssignMemberRequest(operatorId, MembershipStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/admin/municipalities/{municipalityId}/members", munId)
                        .with(authentication(createAuthToken(adminId, "MUNICIPAL_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(memberId.toString()))
                .andExpect(jsonPath("$.data.userId").value(operatorId.toString()))
                .andExpect(jsonPath("$.data.municipalityId").value(munId.toString()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("ADM-004: Deve retornar membros do município quando operador possui escopo ativo")
    void shouldListMembersWhenUserHasScope() throws Exception {
        UUID operatorId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();

        MunicipalityMemberData memberData = new MunicipalityMemberData(
                UUID.randomUUID(), operatorId, "Operador João", "joao@niteroi.rj.gov.br",
                munId, "ACTIVE", Instant.now()
        );

        when(municipalityService.listMembers(munId, operatorId))
                .thenReturn(List.of(memberData));

        mockMvc.perform(get("/api/v1/admin/municipalities/{municipalityId}/members", munId)
                        .with(authentication(createAuthToken(operatorId, "MUNICIPAL_OPERATOR")))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(operatorId.toString()))
                .andExpect(jsonPath("$.data[0].municipalityId").value(munId.toString()));
    }

    @Test
    @DisplayName("ADM-005: RN001 / RBAC-002: Deve negar acesso (403 FORBIDDEN) quando operador tenta acessar dados de outro município")
    void shouldDenyAccessWhenOperatorLacksScopeInMunicipality() throws Exception {
        UUID operatorId = UUID.randomUUID();
        UUID targetMunId = UUID.randomUUID();

        when(municipalityService.listMembers(targetMunId, operatorId))
                .thenThrow(new AccessDeniedException("Usuário não possui vínculo ativo nem permissão para operar no município especificado."));

        mockMvc.perform(get("/api/v1/admin/municipalities/{municipalityId}/members", targetMunId)
                        .with(authentication(createAuthToken(operatorId, "MUNICIPAL_OPERATOR")))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Usuário não possui vínculo ativo nem permissão para operar no município especificado."));
    }
}
