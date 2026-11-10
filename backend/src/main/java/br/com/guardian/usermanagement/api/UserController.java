package br.com.guardian.usermanagement.api;

import br.com.guardian.shared.api.dto.ApiResponse;
import br.com.guardian.shared.security.UserPrincipal;
import br.com.guardian.usermanagement.api.dto.UpdateProfileRequest;
import br.com.guardian.usermanagement.api.dto.UserProfileData;
import br.com.guardian.usermanagement.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuários", description = "Endpoints de consulta e gerenciamento de perfil do usuário")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar perfil autenticado", description = "Retorna os dados do próprio usuário autenticado.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Perfil recuperado com sucesso."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Não autenticado.")
    })
    public ResponseEntity<ApiResponse<UserProfileData>> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        UserProfileData data = userService.getMyProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @PatchMapping("/me")
    @Operation(summary = "Atualizar perfil autenticado", description = "Atualiza nome ou preferências de idioma do usuário.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Não autenticado."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Dados inválidos.")
    })
    public ResponseEntity<ApiResponse<UserProfileData>> updateMyProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                                        @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileData data = userService.updateMyProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.of(data));
    }
}
