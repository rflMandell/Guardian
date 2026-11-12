package br.com.guardian.alert.api;

import br.com.guardian.alert.api.dto.AlertResponseData;
import br.com.guardian.alert.application.AlertService;
import br.com.guardian.alert.domain.model.AlertStatus;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alertas", description = "Consulta pública de alertas de emergência e riscos iminentes")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @Operation(summary = "Listar alertas ativos", description = "Retorna alertas publicados e vigentes com filtros por município e severidade.")
    public ResponseEntity<PaginatedResponse<AlertResponseData>> listAlerts(
            @RequestParam(required = false) UUID municipalityId,
            @RequestParam(required = false) RiskLevel severity,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("issuedAt").descending());
        Page<AlertResponseData> pageResult = alertService.listAlerts(municipalityId, severity, status, pageable);
        return ResponseEntity.ok(PaginatedResponse.from(pageResult));
    }

    @GetMapping("/{alertId}")
    @Operation(summary = "Consultar alerta", description = "Retorna detalhes completos do alerta, área de abrangência e recomendações.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Alerta encontrado."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Alerta não encontrado.")
    })
    public ResponseEntity<ApiResponse<AlertResponseData>> getAlert(@PathVariable UUID alertId) {
        AlertResponseData data = alertService.getAlert(alertId);
        return ResponseEntity.ok(ApiResponse.of(data));
    }
}
