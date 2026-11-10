package br.com.guardian.identity.api;

import br.com.guardian.identity.api.dto.*;
import br.com.guardian.identity.application.IdentityService;
import br.com.guardian.shared.exception.BusinessException;
import br.com.guardian.shared.exception.GlobalExceptionHandler;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IdentityService identityService;

    @Test
    @DisplayName("AUTH-001: Deve registrar novo cidadão com sucesso retornando 201 Created")
    void shouldRegisterCitizenSuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();
        RegisterResponseData responseData = new RegisterResponseData(
                userId, "João Silva", "joao@email.com", "CITIZEN", false
        );

        when(identityService.register(any())).thenReturn(responseData);

        RegisterRequest request = new RegisterRequest("João Silva", "joao@email.com", "SenhaForte12345");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.name").value("João Silva"))
                .andExpect(jsonPath("$.data.email").value("joao@email.com"))
                .andExpect(jsonPath("$.data.accountType").value("CITIZEN"))
                .andExpect(jsonPath("$.data.emailVerified").value(false));
    }

    @Test
    @DisplayName("AUTH-002: Deve rejeitar cadastro com e-mail duplicado retornando 422")
    void shouldRejectDuplicateEmail() throws Exception {
        when(identityService.register(any()))
                .thenThrow(new BusinessException("EMAIL_ALREADY_REGISTERED", "O e-mail informado já está cadastrado."));

        RegisterRequest request = new RegisterRequest("João Silva", "duplicado@email.com", "SenhaForte12345");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.message").value("O e-mail informado já está cadastrado."));
    }

    @Test
    @DisplayName("AUTH-003: Deve validar comprimento mínimo de senha de 12 caracteres")
    void shouldRejectShortPassword() throws Exception {
        RegisterRequest request = new RegisterRequest("João Silva", "joao@email.com", "curta123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details[0].field").value("password"))
                .andExpect(jsonPath("$.details[0].message").value("A senha deve ter no mínimo 12 caracteres."));
    }

    @Test
    @DisplayName("AUTH-004: Deve realizar login com sucesso e retornar tokens JWT e refresh")
    void shouldLoginSuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();
        LoginResponseData responseData = new LoginResponseData(
                "jwt.access.token",
                "Bearer",
                900,
                "refresh-token-uuid",
                new LoginUserData(userId, "João Silva", List.of("CITIZEN"))
        );

        when(identityService.login(any())).thenReturn(responseData);

        LoginRequest request = new LoginRequest("joao@email.com", "SenhaForte12345");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("jwt.access.token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-uuid"))
                .andExpect(jsonPath("$.data.user.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.user.name").value("João Silva"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("CITIZEN"));
    }

    @Test
    @DisplayName("AUTH-005: Deve renovar tokens via refresh token com rotação")
    void shouldRefreshTokenSuccessfully() throws Exception {
        TokenRefreshResponseData responseData = new TokenRefreshResponseData(
                "new.jwt.access.token",
                "Bearer",
                900,
                "new-refresh-token-uuid"
        );

        when(identityService.refreshToken(any())).thenReturn(responseData);

        RefreshTokenRequest request = new RefreshTokenRequest("current-refresh-token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new.jwt.access.token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token-uuid"));
    }

    @Test
    @DisplayName("AUTH-006: Deve revogar refresh token no logout retornando 204 No Content")
    void shouldLogoutSuccessfully() throws Exception {
        doNothing().when(identityService).logout(any());

        LogoutRequest request = new LogoutRequest("token-to-revoke");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }
}
