package br.com.guardian.shared.api;

import br.com.guardian.shared.api.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health Check", description = "Verificação de disponibilidade e saúde da aplicação")
public class HealthController {

    @GetMapping
    @Operation(summary = "Verificar integridade da aplicação", description = "Retorna o status operacional da API Guardian.")
    @ApiResponse(responseCode = "200", description = "Aplicação em execução normal.")
    public ResponseEntity<HealthResponse> checkHealth() {
        return ResponseEntity.ok(HealthResponse.up());
    }
}
