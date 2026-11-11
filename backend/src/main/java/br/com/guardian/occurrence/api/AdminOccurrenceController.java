package br.com.guardian.occurrence.api;

import br.com.guardian.occurrence.api.dto.LinkOccurrenceRequest;
import br.com.guardian.occurrence.api.dto.OccurrenceDetailData;
import br.com.guardian.occurrence.api.dto.UpdateOccurrenceStatusRequest;
import br.com.guardian.occurrence.application.OccurrenceService;
import br.com.guardian.shared.api.dto.ApiResponse;
import br.com.guardian.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/occurrences")
@Tag(name = "Administração de Ocorrências", description = "Operações administrativas de validação, atualização e vinculação de relatos a incidentes")
@SecurityRequirement(name = "bearerAuth")
public class AdminOccurrenceController {

    private final OccurrenceService occurrenceService;

    public AdminOccurrenceController(OccurrenceService occurrenceService) {
        this.occurrenceService = occurrenceService;
    }

    @PatchMapping("/{occurrenceId}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN') or hasRole('MUNICIPAL_ADMIN') or hasRole('MUNICIPAL_OPERATOR') or hasAuthority('OCCURRENCE_UPDATE')")
    @Operation(summary = "Atualizar status da ocorrência", description = "Permite ao operador municipal atualizar o status do relato (ex: UNDER_ANALYSIS, DISMISSED, CLOSED).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status atualizado com sucesso."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acesso negado: operador sem escopo municipal."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Relato não encontrado.")
    })
    public ResponseEntity<ApiResponse<OccurrenceDetailData>> updateOccurrenceStatus(
            @PathVariable UUID occurrenceId,
            @Valid @RequestBody UpdateOccurrenceStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID operatorId = principal != null ? principal.getId() : null;
        OccurrenceDetailData data = occurrenceService.updateStatus(occurrenceId, request, operatorId);
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @PostMapping("/{occurrenceId}/link")
    @PreAuthorize("hasRole('GLOBAL_ADMIN') or hasRole('MUNICIPAL_ADMIN') or hasRole('MUNICIPAL_OPERATOR') or hasAuthority('OCCURRENCE_UPDATE')")
    @Operation(summary = "Associar relato a incidente confirmado", description = "Vincula um relato a um incidente principal confirmado sem apagá-lo.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Relato vinculado com sucesso."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acesso negado."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Relato ou incidente não encontrado."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Relato já vinculado ou municípios divergentes.")
    })
    public ResponseEntity<Void> linkToIncident(
            @PathVariable UUID occurrenceId,
            @Valid @RequestBody LinkOccurrenceRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID operatorId = principal != null ? principal.getId() : null;
        occurrenceService.linkToIncident(occurrenceId, request.incidentId(), operatorId);
        return ResponseEntity.noContent().build();
    }
}
