package br.com.guardian.geolocation.api;

import br.com.guardian.geolocation.api.dto.RiskAreaResponseData;
import br.com.guardian.geolocation.application.GeolocationService;
import br.com.guardian.shared.api.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/risk-areas")
@Tag(name = "Áreas de Risco", description = "Consulta pública de regiões vulneráveis monitoradas")
public class RiskAreaController {

    private final GeolocationService geolocationService;

    public RiskAreaController(GeolocationService geolocationService) {
        this.geolocationService = geolocationService;
    }

    @GetMapping
    @Operation(summary = "Listar áreas de risco", description = "Retorna as áreas de risco cadastradas para um município, com filtro opcional por nível de vulnerabilidade.")
    public ResponseEntity<ApiResponse<List<RiskAreaResponseData>>> listRiskAreas(
            @RequestParam UUID municipalityId,
            @RequestParam(required = false) String riskLevel
    ) {
        List<RiskAreaResponseData> data = geolocationService.listRiskAreas(municipalityId, riskLevel);
        return ResponseEntity.ok(ApiResponse.of(data));
    }
}
