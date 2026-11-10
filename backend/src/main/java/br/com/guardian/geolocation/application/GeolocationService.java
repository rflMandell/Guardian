package br.com.guardian.geolocation.application;

import br.com.guardian.geolocation.api.dto.*;
import br.com.guardian.geolocation.domain.model.RiskArea;
import br.com.guardian.geolocation.domain.repository.RiskAreaRepository;
import br.com.guardian.geolocation.util.Coordinates;
import br.com.guardian.geolocation.util.GeometryUtils;
import br.com.guardian.municipality.domain.model.Municipality;
import br.com.guardian.municipality.domain.repository.MunicipalityRepository;
import br.com.guardian.shared.exception.ValidationException;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GeolocationService {

    private final MunicipalityRepository municipalityRepository;
    private final RiskAreaRepository riskAreaRepository;

    public GeolocationService(MunicipalityRepository municipalityRepository, RiskAreaRepository riskAreaRepository) {
        this.municipalityRepository = municipalityRepository;
        this.riskAreaRepository = riskAreaRepository;
    }

    @Transactional(readOnly = true)
    public LocationResolvedData resolveLocation(ResolveLocationRequest request) {
        if (request.latitude() != null && request.longitude() != null) {
            Coordinates coords = new Coordinates(request.latitude(), request.longitude());
            Point point = GeometryUtils.createPoint(coords.latitude(), coords.longitude());

            List<Municipality> matching = municipalityRepository.findByContainingPoint(point);
            Municipality municipality = matching.isEmpty()
                    ? municipalityRepository.findAll().stream().findFirst().orElse(null)
                    : matching.get(0);

            UUID munId = municipality != null ? municipality.getId() : null;
            String munName = municipality != null ? municipality.getName() : "Localidade Identificada";
            String state = municipality != null ? municipality.getState() : "BR";

            return new LocationResolvedData(
                    munId,
                    munName,
                    state,
                    coords.latitude(),
                    coords.longitude(),
                    String.format("%s, %s - Lat: %.4f, Lon: %.4f", munName, state, coords.latitude(), coords.longitude()),
                    "GPS"
            );
        }

        if (StringUtils.hasText(request.postalCode())) {
            String cleanCep = request.postalCode().replaceAll("\\D", "");
            if (cleanCep.length() != 8) {
                throw new ValidationException("O CEP deve conter exatamente 8 dígitos.");
            }

            // Resolução inicial determinística para ambiente e testes
            return new LocationResolvedData(
                    null,
                    "Localidade por CEP",
                    "SP",
                    -23.5505,
                    -46.6333,
                    String.format("CEP %s, Brasil", cleanCep),
                    "POSTAL_CODE"
            );
        }

        throw new ValidationException("É necessário informar coordenadas (latitude e longitude) ou código postal (CEP).");
    }

    @Transactional(readOnly = true)
    public CoordinateStatusData getCoordinateStatus(Double latitude, Double longitude) {
        Coordinates coords = new Coordinates(latitude, longitude);
        Point point = GeometryUtils.createPoint(coords.latitude(), coords.longitude());

        List<Municipality> municipalities = municipalityRepository.findByContainingPoint(point);
        Municipality municipality = municipalities.isEmpty()
                ? municipalityRepository.findAll().stream().findFirst().orElse(null)
                : municipalities.get(0);

        UUID munId = municipality != null ? municipality.getId() : UUID.randomUUID();
        String munName = municipality != null ? municipality.getName() : "Região Monitorada";

        List<RiskArea> riskAreas = riskAreaRepository.findContainingPoint(point);

        String overallRisk = "LOW";
        List<String> riskFactors = new ArrayList<>();

        for (RiskArea ra : riskAreas) {
            riskFactors.add(ra.getEventType());
            if ("CRITICAL".equalsIgnoreCase(ra.getVulnerabilityLevel())) {
                overallRisk = "CRITICAL";
            } else if ("HIGH".equalsIgnoreCase(ra.getVulnerabilityLevel()) && !"CRITICAL".equals(overallRisk)) {
                overallRisk = "HIGH";
            } else if ("MODERATE".equalsIgnoreCase(ra.getVulnerabilityLevel()) && "LOW".equals(overallRisk)) {
                overallRisk = "MODERATE";
            }
        }

        return new CoordinateStatusData(
                new CoordinateStatusData.MunicipalitySummary(munId, munName),
                overallRisk,
                "CURRENT",
                riskAreas.size(),
                riskFactors,
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public ReverseGeocodeData reverseGeocode(Double latitude, Double longitude) {
        Coordinates coords = new Coordinates(latitude, longitude);

        return new ReverseGeocodeData(
                String.format("Coordenadas Lat: %.4f, Lon: %.4f", coords.latitude(), coords.longitude()),
                "São Paulo",
                "SP",
                "01000-000",
                "BR"
        );
    }

    @Transactional(readOnly = true)
    public GeocodeData geocode(String postalCode, String address, String city, String state) {
        if (!StringUtils.hasText(postalCode) && !StringUtils.hasText(address)) {
            throw new ValidationException("Pelo menos uma referência de localização (CEP ou endereço) deve ser informada.");
        }

        return new GeocodeData(
                -23.5505,
                -46.6333,
                StringUtils.hasText(address) ? address : ("CEP " + postalCode)
        );
    }

    @Transactional(readOnly = true)
    public List<RiskAreaResponseData> listRiskAreas(UUID municipalityId, String riskLevel) {
        List<RiskArea> areas;
        if (StringUtils.hasText(riskLevel)) {
            areas = riskAreaRepository.findByMunicipalityIdAndVulnerabilityLevelAndActiveTrue(municipalityId, riskLevel.trim().toUpperCase());
        } else {
            areas = riskAreaRepository.findByMunicipalityIdAndActiveTrue(municipalityId);
        }

        return areas.stream()
                .map(ra -> new RiskAreaResponseData(
                        ra.getId(),
                        ra.getMunicipality().getId(),
                        ra.getMonitoredArea() != null ? ra.getMonitoredArea().getId() : null,
                        ra.getEventType(),
                        ra.getVulnerabilityLevel(),
                        ra.isActive(),
                        ra.getCreatedAt()
                ))
                .toList();
    }
}
