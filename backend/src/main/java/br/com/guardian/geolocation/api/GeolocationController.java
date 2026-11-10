package br.com.guardian.geolocation.api;

import br.com.guardian.geolocation.api.dto.*;
import br.com.guardian.geolocation.application.GeolocationService;
import br.com.guardian.shared.api.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/geolocation")
@Tag(name = "Geolocalização", description = "Resolução geográfica, geocodificação e consulta de situação territorial por coordenadas")
public class GeolocationController {

    private final GeolocationService geolocationService;

    public GeolocationController(GeolocationService geolocationService) {
        this.geolocationService = geolocationService;
    }

    @PostMapping("/resolve")
    @Operation(summary = "Resolver localização", description = "Identifica a localidade ou município a partir de coordenadas GPS ou código postal (CEP).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Localização resolvida."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Coordenadas ou CEP inválidos.")
    })
    public ResponseEntity<ApiResponse<LocationResolvedData>> resolve(@RequestBody ResolveLocationRequest request) {
        LocationResolvedData data = geolocationService.resolveLocation(request);
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @GetMapping("/status")
    @Operation(summary = "Consultar situação de uma coordenada", description = "Retorna o município, nível de risco e fatores de ameaça associados a uma posição geográfica.")
    public ResponseEntity<ApiResponse<CoordinateStatusData>> getStatus(
            @RequestParam Double latitude,
            @RequestParam Double longitude
    ) {
        CoordinateStatusData data = geolocationService.getCoordinateStatus(latitude, longitude);
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @GetMapping("/reverse-geocode")
    @Operation(summary = "Reverse geocoding", description = "Identifica endereço aproximado a partir de coordenadas.")
    public ResponseEntity<ApiResponse<ReverseGeocodeData>> reverseGeocode(
            @RequestParam Double latitude,
            @RequestParam Double longitude
    ) {
        ReverseGeocodeData data = geolocationService.reverseGeocode(latitude, longitude);
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @GetMapping("/geocode")
    @Operation(summary = "Geocoding", description = "Identifica coordenadas a partir de CEP ou endereço textual.")
    public ResponseEntity<ApiResponse<GeocodeData>> geocode(
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state
    ) {
        GeocodeData data = geolocationService.geocode(postalCode, address, city, state);
        return ResponseEntity.ok(ApiResponse.of(data));
    }
}
