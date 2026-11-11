package br.com.guardian.occurrence.api;

import br.com.guardian.occurrence.api.dto.*;
import br.com.guardian.occurrence.application.OccurrenceService;
import br.com.guardian.occurrence.domain.model.EventCategory;
import br.com.guardian.occurrence.domain.model.ReportStatus;
import br.com.guardian.shared.api.dto.ApiResponse;
import br.com.guardian.shared.api.dto.PaginatedResponse;
import br.com.guardian.shared.security.UserPrincipal;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/occurrences")
@Tag(name = "Ocorrências", description = "Registro e consulta pública de relatos de ocorrências e eventos adversos")
public class OccurrenceController {

    private final OccurrenceService occurrenceService;

    public OccurrenceController(OccurrenceService occurrenceService) {
        this.occurrenceService = occurrenceService;
    }

    @PostMapping
    @Operation(summary = "Registrar ocorrência", description = "Permite que cidadãos (autenticados ou anônimos) registrem situações potencialmente perigosas.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Relato registrado com sucesso."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Dados inválidos, coordenadas fora do padrão ou município não monitorado.")
    })
    public ResponseEntity<ApiResponse<OccurrenceResponseData>> createOccurrence(
            @Valid @RequestBody CreateOccurrenceRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID authenticatedUserId = principal != null ? principal.getId() : null;
        OccurrenceResponseData data = occurrenceService.createReport(request, authenticatedUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
    }

    @GetMapping
    @Operation(summary = "Listar ocorrências", description = "Retorna lista paginada de relatos preservando a privacidade dos autores.")
    public ResponseEntity<PaginatedResponse<OccurrenceSummaryData>> listOccurrences(
            @RequestParam(required = false) UUID municipalityId,
            @RequestParam(required = false) EventCategory eventCategory,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radiusMeters,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("reportedAt").descending());
        Page<OccurrenceSummaryData> pageResult = occurrenceService.listReports(
                municipalityId, eventCategory, status, latitude, longitude, radiusMeters, from, to, pageable
        );
        return ResponseEntity.ok(PaginatedResponse.from(pageResult));
    }

    @GetMapping("/{occurrenceId}")
    @Operation(summary = "Consultar ocorrência", description = "Retorna detalhes públicos de um relato sem expor dados pessoais do autor.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Relato encontrado."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Relato não encontrado.")
    })
    public ResponseEntity<ApiResponse<OccurrenceDetailData>> getOccurrence(@PathVariable UUID occurrenceId) {
        OccurrenceDetailData data = occurrenceService.getReport(occurrenceId);
        return ResponseEntity.ok(ApiResponse.of(data));
    }
}
