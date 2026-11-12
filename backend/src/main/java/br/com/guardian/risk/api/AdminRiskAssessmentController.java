package br.com.guardian.risk.api;

import br.com.guardian.risk.api.dto.RecalculateResponseData;
import br.com.guardian.risk.api.dto.RecalculateRiskRequest;
import br.com.guardian.risk.application.RiskAssessmentService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/risk-assessments")
@Tag(name = "Administração de Risco", description = "Operações administrativas de cálculo e reavaliação de riscos territoriais")
@SecurityRequirement(name = "bearerAuth")
public class AdminRiskAssessmentController {

    private final RiskAssessmentService riskAssessmentService;

    public AdminRiskAssessmentController(RiskAssessmentService riskAssessmentService) {
        this.riskAssessmentService = riskAssessmentService;
    }

    @PostMapping("/recalculate")
    @PreAuthorize("hasRole('GLOBAL_ADMIN') or hasRole('MUNICIPAL_ADMIN') or hasRole('MUNICIPAL_OPERATOR') or hasAuthority('RISK_ASSESSMENT_CREATE')")
    @Operation(summary = "Criar ou recalcular avaliação de risco", description = "Solicita o cálculo explicável de risco territorial para um município e categoria de evento.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Avaliação solicitada para processamento."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acesso negado: operador sem escopo municipal."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Dados inválidos ou área de risco não correspondente ao município.")
    })
    public ResponseEntity<ApiResponse<RecalculateResponseData>> recalculate(
            @Valid @RequestBody RecalculateRiskRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID operatorId = principal != null ? principal.getId() : null;
        RecalculateResponseData data = riskAssessmentService.recalculateRisk(request, operatorId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.of(data));
    }
}
