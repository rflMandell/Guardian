package br.com.guardian.safelocation.api;

import br.com.guardian.occurrence.api.dto.CoordinatesDto;
import br.com.guardian.safelocation.api.dto.CreateSafeLocationRequest;
import br.com.guardian.safelocation.api.dto.SafeLocationResponseData;
import br.com.guardian.safelocation.api.dto.UpdateSafeLocationAvailabilityRequest;
import br.com.guardian.safelocation.application.SafeLocationService;
import br.com.guardian.safelocation.domain.model.SafeLocationAvailability;
import br.com.guardian.safelocation.domain.model.SafeLocationOperationalStatus;
import br.com.guardian.safelocation.domain.model.SafeLocationType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminSafeLocationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("AdminSafeLocationController - Testes administrativos de gestão de pontos seguros")
class AdminSafeLocationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SafeLocationService safeLocationService;

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
    @DisplayName("ADM-SAFE-001: Deve permitir que operador municipal cadastre ponto seguro retornando 201 Created")
    void shouldAllowOperatorToCreateSafeLocation() throws Exception {
        UUID operatorId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();
        UUID locId = UUID.randomUUID();

        SafeLocationResponseData responseData = new SafeLocationResponseData(
                locId, munId, "Abrigo Municipal", "Abrigo",
                SafeLocationType.SHELTER, "Rua do Acolhimento, 100",
                new CoordinatesDto(-22.505, -43.18), 200, 0,
                SafeLocationAvailability.AVAILABLE, true,
                SafeLocationOperationalStatus.OPEN, Instant.now(), "CURRENT"
        );

        when(safeLocationService.createSafeLocation(any(CreateSafeLocationRequest.class), eq(operatorId)))
                .thenReturn(responseData);

        CreateSafeLocationRequest request = new CreateSafeLocationRequest(
                munId, "Abrigo Municipal", "Abrigo", SafeLocationType.SHELTER,
                "Rua do Acolhimento, 100", -22.505, -43.18, 200,
                true, SafeLocationOperationalStatus.OPEN
        );

        mockMvc.perform(post("/api/v1/admin/safe-locations")
                        .with(authentication(createAuthToken(operatorId, "MUNICIPAL_OPERATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(locId.toString()))
                .andExpect(jsonPath("$.data.name").value("Abrigo Municipal"))
                .andExpect(jsonPath("$.data.availability").value("AVAILABLE"));
    }

    @Test
    @DisplayName("ADM-SAFE-002: Deve permitir que operador municipal atualize ocupação retornando 200 OK")
    void shouldAllowOperatorToUpdateAvailability() throws Exception {
        UUID operatorId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();
        UUID locId = UUID.randomUUID();

        SafeLocationResponseData responseData = new SafeLocationResponseData(
                locId, munId, "Abrigo Municipal", "Abrigo",
                SafeLocationType.SHELTER, "Rua do Acolhimento, 100",
                new CoordinatesDto(-22.505, -43.18), 200, 150,
                SafeLocationAvailability.AVAILABLE, true,
                SafeLocationOperationalStatus.OPEN, Instant.now(), "CURRENT"
        );

        when(safeLocationService.updateAvailability(eq(locId), any(UpdateSafeLocationAvailabilityRequest.class), eq(operatorId)))
                .thenReturn(responseData);

        UpdateSafeLocationAvailabilityRequest request = new UpdateSafeLocationAvailabilityRequest(
                150, SafeLocationOperationalStatus.OPEN
        );

        mockMvc.perform(patch("/api/v1/admin/safe-locations/{safeLocationId}/availability", locId)
                        .with(authentication(createAuthToken(operatorId, "MUNICIPAL_OPERATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.occupancy").value(150))
                .andExpect(jsonPath("$.data.availability").value("AVAILABLE"));
    }

    @Test
    @DisplayName("ADM-SAFE-003: Deve negar cadastro de ponto seguro para cidadão comum (403 Forbidden)")
    void shouldDenyCreateSafeLocationForCitizen() throws Exception {
        UUID citizenId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();

        CreateSafeLocationRequest request = new CreateSafeLocationRequest(
                munId, "Ponto Fake", "Descrição", SafeLocationType.SHELTER,
                "Rua", -22.505, -43.18, 100, true, SafeLocationOperationalStatus.OPEN
        );

        mockMvc.perform(post("/api/v1/admin/safe-locations")
                        .with(authentication(createAuthToken(citizenId, "CITIZEN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADM-SAFE-004: RN001: Deve negar cadastro quando operador não possui escopo no município")
    void shouldDenyCreateSafeLocationWhenOperatorLacksScope() throws Exception {
        UUID operatorId = UUID.randomUUID();
        UUID munId = UUID.randomUUID();

        when(safeLocationService.createSafeLocation(any(CreateSafeLocationRequest.class), eq(operatorId)))
                .thenThrow(new AccessDeniedException("Operador não possui escopo no município."));

        CreateSafeLocationRequest request = new CreateSafeLocationRequest(
                munId, "Abrigo", "Desc", SafeLocationType.SHELTER,
                "Rua", -22.505, -43.18, 100, true, SafeLocationOperationalStatus.OPEN
        );

        mockMvc.perform(post("/api/v1/admin/safe-locations")
                        .with(authentication(createAuthToken(operatorId, "MUNICIPAL_OPERATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
