package br.com.guardian.risk.api;

import br.com.guardian.occurrence.domain.model.EventCategory;
import br.com.guardian.risk.api.dto.RiskAssessmentResponseData;
import br.com.guardian.risk.application.RiskAssessmentService;
import br.com.guardian.risk.domain.model.RiskLevel;
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

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/risk-assessments")
@Tag(name = "Avaliações de Risco", description = "Consulta pública de avaliações de risco territorial e fatores explicativos")
public class RiskAssessmentController {

    private final RiskAssessmentService riskAssessmentService;

    public RiskAssessmentController(RiskAssessmentService riskAssessmentService) {
        this.riskAssessmentService = riskAssessmentService;
    }

    @GetMapping
    @Operation(summary = "Listar avaliações de risco", description = "Retorna avaliações de risco com filtros por município, área, categoria e nível de risco.")
    public ResponseEntity<PaginatedResponse<RiskAssessmentResponseData>> listAssessments(
            @RequestParam(required = false) UUID municipalityId,
            @RequestParam(required = false) UUID riskAreaId,
            @RequestParam(required = false) EventCategory eventCategory,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("evaluatedAt").descending());
        Page<RiskAssessmentResponseData> pageResult = riskAssessmentService.listAssessments(
                municipalityId, riskAreaId, eventCategory, riskLevel, from, to, pageable
        );
        return ResponseEntity.ok(PaginatedResponse.from(pageResult));
    }

    @GetMapping("/{assessmentId}")
    @Operation(summary = "Consultar avaliação de risco", description = "Retorna os detalhes e os fatores rastreáveis de uma avaliação de risco específica.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Avaliação encontrada."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Avaliação não encontrada.")
    })
    public ResponseEntity<ApiResponse<RiskAssessmentResponseData>> getAssessment(@PathVariable UUID assessmentId) {
        RiskAssessmentResponseData data = riskAssessmentService.getAssessment(assessmentId);
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @GetMapping("/latest")
    @Operation(summary = "Consultar avaliação mais recente", description = "Retorna a avaliação mais recente para um determinado município e categoria de evento.")
    public ResponseEntity<ApiResponse<RiskAssessmentResponseData>> getLatest(
            @RequestParam UUID municipalityId,
            @RequestParam(required = false) EventCategory eventCategory
    ) {
        RiskAssessmentResponseData data = riskAssessmentService.getLatestAssessment(municipalityId, eventCategory);
        return ResponseEntity.ok(ApiResponse.of(data));
    }
}
