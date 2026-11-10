package br.com.guardian.municipality.api;

import br.com.guardian.municipality.api.dto.AssignMemberRequest;
import br.com.guardian.municipality.api.dto.CreateMunicipalityRequest;
import br.com.guardian.municipality.api.dto.MunicipalityMemberData;
import br.com.guardian.municipality.api.dto.MunicipalityResponseData;
import br.com.guardian.municipality.application.MunicipalityService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/municipalities")
@Tag(name = "Administração Municipal", description = "Operações administrativas e gestão de membros por município")
@SecurityRequirement(name = "bearerAuth")
public class AdminMunicipalityController {

    private final MunicipalityService municipalityService;

    public AdminMunicipalityController(MunicipalityService municipalityService) {
        this.municipalityService = municipalityService;
    }

    @PostMapping
    @PreAuthorize("hasRole('GLOBAL_ADMIN') or hasAuthority('MUNICIPALITY_CREATE')")
    @Operation(summary = "Cadastrar município (Administração)", description = "Operação restrita a administradores autorizados.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Município cadastrado com sucesso."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Sem permissão."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Código IBGE duplicado ou dados inválidos.")
    })
    public ResponseEntity<ApiResponse<MunicipalityResponseData>> createMunicipality(@Valid @RequestBody CreateMunicipalityRequest request) {
        MunicipalityResponseData data = municipalityService.createMunicipality(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
    }

    @PostMapping("/{municipalityId}/members")
    @PreAuthorize("hasRole('GLOBAL_ADMIN') or hasRole('MUNICIPAL_ADMIN') or hasAuthority('ROLE_MANAGE')")
    @Operation(summary = "Vincular operador ao município", description = "Associa um usuário operador ou administrador ao escopo municipal.")
    public ResponseEntity<ApiResponse<MunicipalityMemberData>> assignMember(
            @PathVariable UUID municipalityId,
            @Valid @RequestBody AssignMemberRequest request
    ) {
        MunicipalityMemberData data = municipalityService.assignMember(municipalityId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
    }

    @GetMapping("/{municipalityId}/members")
    @Operation(summary = "Listar membros do município", description = "Retorna os operadores vinculados ao município. Aplica isolamento municipal estrito.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Membros retornados."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acesso negado: operador não possui escopo neste município.")
    })
    public ResponseEntity<ApiResponse<List<MunicipalityMemberData>>> listMembers(
            @PathVariable UUID municipalityId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<MunicipalityMemberData> data = municipalityService.listMembers(municipalityId, principal.getId());
        return ResponseEntity.ok(ApiResponse.of(data));
    }
}
