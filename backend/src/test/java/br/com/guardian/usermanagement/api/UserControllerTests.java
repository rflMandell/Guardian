package br.com.guardian.usermanagement.api;

import br.com.guardian.shared.security.SecurityConfig;
import br.com.guardian.shared.security.UserPrincipal;
import br.com.guardian.usermanagement.api.dto.UpdateProfileRequest;
import br.com.guardian.usermanagement.api.dto.UserProfileData;
import br.com.guardian.usermanagement.application.UserService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private br.com.guardian.shared.security.JwtTokenProvider tokenProvider;

    @MockBean
    private br.com.guardian.shared.security.CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("SEC-001: Deve recusar acesso a /api/v1/users/me sem autenticação retornando 401 UNAUTHORIZED")
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/me"));
    }

    @Test
    @DisplayName("USER-001: Deve retornar o perfil do usuário autenticado")
    void shouldReturnAuthenticatedUserProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId, "João Silva", "joao@email.com", "hash",
                AccountType.CITIZEN, UserStatus.ACTIVE,
                List.of(new SimpleGrantedAuthority("ROLE_CITIZEN"))
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        UserProfileData profileData = new UserProfileData(
                userId, "João Silva", "joao@email.com", "CITIZEN", "pt-BR", true, List.of("CITIZEN")
        );

        when(userService.getMyProfile(eq(userId))).thenReturn(profileData);

        mockMvc.perform(get("/api/v1/users/me")
                        .with(authentication(auth))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.name").value("João Silva"))
                .andExpect(jsonPath("$.data.email").value("joao@email.com"))
                .andExpect(jsonPath("$.data.accountType").value("CITIZEN"))
                .andExpect(jsonPath("$.data.preferredLanguage").value("pt-BR"))
                .andExpect(jsonPath("$.data.emailVerified").value(true));
    }

    @Test
    @DisplayName("USER-002: Deve permitir atualizar nome e idioma do perfil autenticado")
    void shouldUpdateUserProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId, "João Silva", "joao@email.com", "hash",
                AccountType.CITIZEN, UserStatus.ACTIVE,
                List.of(new SimpleGrantedAuthority("ROLE_CITIZEN"))
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        UpdateProfileRequest request = new UpdateProfileRequest("João da Silva Atualizado", "en");

        UserProfileData updatedData = new UserProfileData(
                userId, "João da Silva Atualizado", "joao@email.com", "CITIZEN", "en", true, List.of("CITIZEN")
        );

        when(userService.updateMyProfile(eq(userId), any())).thenReturn(updatedData);

        mockMvc.perform(patch("/api/v1/users/me")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("João da Silva Atualizado"))
                .andExpect(jsonPath("$.data.preferredLanguage").value("en"));
    }
}
