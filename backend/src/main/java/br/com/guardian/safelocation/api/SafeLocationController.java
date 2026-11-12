package br.com.guardian.safelocation.api;

import br.com.guardian.safelocation.api.dto.SafeLocationResponseData;
import br.com.guardian.safelocation.application.SafeLocationService;
import br.com.guardian.safelocation.domain.model.SafeLocationAvailability;
import br.com.guardian.safelocation.domain.model.SafeLocationType;
import br.com.guardian.shared.api.dto.ApiResponse;
import br.com.guardian.shared.api.dto.PaginatedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/safe-locations")
@Tag(name = "Pontos Seguros", description = "Consulta pública de abrigos, ginásios, escolas e locais seguros de apoio à evacuação")
public class SafeLocationController {

    private final SafeLocationService safeLocationService;

    public SafeLocationController(SafeLocationService safeLocationService) {
        this.safeLocationService = safeLocationService;
    }

    @GetMapping
    @Operation(summary = "Consultar pontos seguros", description = "Retorna lista de pontos seguros com filtros de acessibilidade, tipo, capacidade e proximidade.")
    public ResponseEntity<PaginatedResponse<SafeLocationResponseData>> listSafeLocations(
            @RequestParam(required = false) UUID municipalityId,
            @RequestParam(required = false) SafeLocationType type,
            @RequestParam(required = false) Boolean accessibility,
            @RequestParam(required = false) SafeLocationAvailability availability,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radiusMeters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<SafeLocationResponseData> pageResult = safeLocationService.listSafeLocations(
                municipalityId, type, accessibility, availability, latitude, longitude, radiusMeters, pageable
        );
        return ResponseEntity.ok(PaginatedResponse.from(pageResult));
    }

    @GetMapping("/{safeLocationId}")
    @Operation(summary = "Consultar ponto seguro", description = "Retorna informações detalhadas de um abrigo ou ponto de segurança específico.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ponto seguro encontrado."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ponto seguro não encontrado.")
    })
    public ResponseEntity<ApiResponse<SafeLocationResponseData>> getSafeLocation(@PathVariable UUID safeLocationId) {
        SafeLocationResponseData data = safeLocationService.getSafeLocation(safeLocationId);
        return ResponseEntity.ok(ApiResponse.of(data));
    }
}
