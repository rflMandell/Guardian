package br.com.guardian.municipality.api;

import br.com.guardian.municipality.api.dto.CreateMunicipalityRequest;
import br.com.guardian.municipality.api.dto.MunicipalityResponseData;
import br.com.guardian.municipality.api.dto.MunicipalityStatusResponseData;
import br.com.guardian.municipality.application.MunicipalityService;
import br.com.guardian.shared.api.dto.ApiResponse;
import br.com.guardian.shared.api.dto.PaginatedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/municipalities")
@Tag(name = "Municípios", description = "Consulta pública e gestão de municípios suportados pela plataforma")
public class MunicipalityController {

    private final MunicipalityService municipalityService;

    public MunicipalityController(MunicipalityService municipalityService) {
        this.municipalityService = municipalityService;
    }

    @GetMapping
    @Operation(summary = "Listar municípios disponíveis", description = "Retorna uma lista paginada de municípios com suporte a busca e filtro por estado.")
    public ResponseEntity<PaginatedResponse<MunicipalityResponseData>> listMunicipalities(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<MunicipalityResponseData> pageResult = municipalityService.listMunicipalities(search, state, pageable);
        return ResponseEntity.ok(PaginatedResponse.from(pageResult));
    }

    @GetMapping("/{municipalityId}")
    @Operation(summary = "Consultar município", description = "Retorna as informações cadastrais de um município específico.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Município encontrado."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Município não encontrado.")
    })
    public ResponseEntity<ApiResponse<MunicipalityResponseData>> getMunicipality(@PathVariable UUID municipalityId) {
        MunicipalityResponseData data = municipalityService.getMunicipality(municipalityId);
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @GetMapping("/{municipalityId}/status")
    @Operation(summary = "Consultar situação municipal", description = "Retorna o status geral de monitoramento, nível de risco e alertas do município.")
    public ResponseEntity<ApiResponse<MunicipalityStatusResponseData>> getMunicipalityStatus(@PathVariable UUID municipalityId) {
        MunicipalityStatusResponseData data = municipalityService.getMunicipalityStatus(municipalityId);
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @PostMapping
    @Operation(summary = "Cadastrar município", description = "Cadastra um novo município na plataforma.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Município criado com sucesso."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Dados inválidos ou código IBGE já cadastrado.")
    })
    public ResponseEntity<ApiResponse<MunicipalityResponseData>> createMunicipality(@Valid @RequestBody CreateMunicipalityRequest request) {
        MunicipalityResponseData data = municipalityService.createMunicipality(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
    }
}
