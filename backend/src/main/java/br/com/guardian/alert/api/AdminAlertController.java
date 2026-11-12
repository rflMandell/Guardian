package br.com.guardian.alert.api;

import br.com.guardian.alert.api.dto.AlertResponseData;
import br.com.guardian.alert.api.dto.CloseAlertRequest;
import br.com.guardian.alert.api.dto.CreateAlertRequest;
import br.com.guardian.alert.api.dto.UpdateAlertRequest;
import br.com.guardian.alert.application.AlertService;
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
@RequestMapping("/api/v1/admin/alerts")
@Tag(name = "Administração de Alertas", description = "Emissão, atualização e encerramento de alertas municipais")
@SecurityRequirement(name = "bearerAuth")
public class AdminAlertController {

    private final AlertService alertService;

    public AdminAlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    @PreAuthorize("hasRole('GLOBAL_ADMIN') or hasRole('MUNICIPAL_ADMIN') or hasRole('MUNICIPAL_OPERATOR') or hasAuthority('ALERT_CREATE')")
    @Operation(summary = "Criar e publicar alerta", description = "Emite um alerta municipal com severidade, recomendações e abrangência geográfica.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Alerta criado e publicado com sucesso."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acesso negado: operador sem escopo no município."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Dados inválidos.")
    })
    public ResponseEntity<ApiResponse<AlertResponseData>> createAlert(
            @Valid @RequestBody CreateAlertRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID operatorId = principal != null ? principal.getId() : null;
        AlertResponseData data = alertService.createAlert(request, operatorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
    }

    @PatchMapping("/{alertId}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN') or hasRole('MUNICIPAL_ADMIN') or hasRole('MUNICIPAL_OPERATOR') or hasAuthority('ALERT_UPDATE')")
    @Operation(summary = "Atualizar alerta", description = "Atualiza severidade, mensagem ou validade preservando histórico de estados.")
    public ResponseEntity<ApiResponse<AlertResponseData>> updateAlert(
            @PathVariable UUID alertId,
            @Valid @RequestBody UpdateAlertRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID operatorId = principal != null ? principal.getId() : null;
        AlertResponseData data = alertService.updateAlert(alertId, request, operatorId);
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @PostMapping("/{alertId}/close")
    @PreAuthorize("hasRole('GLOBAL_ADMIN') or hasRole('MUNICIPAL_ADMIN') or hasRole('MUNICIPAL_OPERATOR') or hasAuthority('ALERT_CLOSE')")
    @Operation(summary = "Encerrar alerta", description = "Encerra um alerta formalmente com justificativa e registro de autoria.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Alerta encerrado com sucesso."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acesso negado."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Alerta não encontrado.")
    })
    public ResponseEntity<Void> closeAlert(
            @PathVariable UUID alertId,
            @Valid @RequestBody CloseAlertRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID operatorId = principal != null ? principal.getId() : null;
        alertService.closeAlert(alertId, request, operatorId);
        return ResponseEntity.noContent().build();
    }
}
