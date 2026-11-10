package br.com.guardian.identity.api;

import br.com.guardian.identity.api.dto.*;
import br.com.guardian.identity.application.IdentityService;
import br.com.guardian.shared.api.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Endpoints de cadastro, login, renovação e encerramento de sessão")
public class AuthController {

    private final IdentityService identityService;

    public AuthController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping("/register")
    @Operation(summary = "Cadastrar novo cidadão", description = "Cria uma conta de cidadão pública com senha segura.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Dados inválidos ou e-mail já cadastrado.")
    })
    public ResponseEntity<ApiResponse<RegisterResponseData>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponseData data = identityService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário", description = "Valida credenciais e emite access token JWT e refresh token.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Autenticação concluída."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Credenciais inválidas ou conta inativa.")
    })
    public ResponseEntity<ApiResponse<LoginResponseData>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponseData data = identityService.login(request);
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar access token", description = "Gera um novo access token rotacionando o refresh token.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token renovado com sucesso."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Refresh token inválido, expirado ou revogado.")
    })
    public ResponseEntity<ApiResponse<TokenRefreshResponseData>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenRefreshResponseData data = identityService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerrar sessão", description = "Revoga o refresh token informado.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Sessão revogada com sucesso.")
    })
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        identityService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
