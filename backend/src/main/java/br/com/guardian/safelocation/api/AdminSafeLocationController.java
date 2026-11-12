package br.com.guardian.safelocation.api;

import br.com.guardian.safelocation.api.dto.CreateSafeLocationRequest;
import br.com.guardian.safelocation.api.dto.SafeLocationResponseData;
import br.com.guardian.safelocation.api.dto.UpdateSafeLocationAvailabilityRequest;
import br.com.guardian.safelocation.application.SafeLocationService;
import br.com.guardian.shared.api.dto.ApiResponse;
import br.com.guardian.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/safe-locations")
@Tag(name = "Administração de Pontos Seguros", description = "Cadastro e gestão de abrigos, capacidade e disponibilidade operacional")
@SecurityRequirement(name = "bearerAuth")
public class AdminSafeLocationController {

    private final SafeLocationService safeLocationService;

    public AdminSafeLocationController(SafeLocationService safeLocationService) {
        this.safeLocationService = safeLocationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('GLOBAL_ADMIN') or hasRole('MUNICIPAL_ADMIN') or hasRole('MUNICIPAL_OPERATOR') or hasAuthority('SAFE_LOCATION_MANAGE')")
    @Operation(summary = "Cadastrar ponto seguro", description = "Registra um novo abrigo, escola ou ginásio seguro com coordenadas e capacidade.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Ponto seguro cadastrado com sucesso."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acesso negado: operador sem escopo municipal."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Dados inválidos ou coordenadas fora do intervalo.")
    })
    public ResponseEntity<ApiResponse<SafeLocationResponseData>> createSafeLocation(
            @Valid @RequestBody CreateSafeLocationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID operatorId = principal != null ? principal.getId() : null;
        SafeLocationResponseData data = safeLocationService.createSafeLocation(request, operatorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
    }

    @PatchMapping("/{safeLocationId}/availability")
    @PreAuthorize("hasRole('GLOBAL_ADMIN') or hasRole('MUNICIPAL_ADMIN') or hasRole('MUNICIPAL_OPERATOR') or hasAuthority('SAFE_LOCATION_MANAGE')")
    @Operation(summary = "Atualizar disponibilidade e ocupação", description = "Atualiza a ocupação em tempo real e o status operacional do ponto seguro.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Disponibilidade atualizada."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acesso negado: operador sem escopo."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Ocupação inconsistente com a capacidade.")
    })
    public ResponseEntity<ApiResponse<SafeLocationResponseData>> updateAvailability(
            @PathVariable UUID safeLocationId,
            @Valid @RequestBody UpdateSafeLocationAvailabilityRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID operatorId = principal != null ? principal.getId() : null;
        SafeLocationResponseData data = safeLocationService.updateAvailability(safeLocationId, request, operatorId);
        return ResponseEntity.ok(ApiResponse.of(data));
    }
}
