package br.com.guardian.safelocation.application;

import br.com.guardian.accesscontrol.application.MunicipalScopeService;
import br.com.guardian.geolocation.util.Coordinates;
import br.com.guardian.geolocation.util.GeometryUtils;
import br.com.guardian.municipality.domain.repository.MunicipalityRepository;
import br.com.guardian.occurrence.api.dto.CoordinatesDto;
import br.com.guardian.occurrence.application.OccurrenceService;
import br.com.guardian.safelocation.api.dto.CreateSafeLocationRequest;
import br.com.guardian.safelocation.api.dto.SafeLocationResponseData;
import br.com.guardian.safelocation.api.dto.UpdateSafeLocationAvailabilityRequest;
import br.com.guardian.safelocation.domain.model.*;
import br.com.guardian.safelocation.domain.repository.SafeLocationOccupancyRepository;
import br.com.guardian.safelocation.domain.repository.SafeLocationRepository;
import br.com.guardian.shared.exception.ResourceNotFoundException;
import br.com.guardian.shared.exception.ValidationException;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SafeLocationService {

    private final SafeLocationRepository safeLocationRepository;
    private final SafeLocationOccupancyRepository occupancyRepository;
    private final MunicipalityRepository municipalityRepository;
    private final MunicipalScopeService municipalScopeService;

    public SafeLocationService(SafeLocationRepository safeLocationRepository,
                               SafeLocationOccupancyRepository occupancyRepository,
                               MunicipalityRepository municipalityRepository,
                               MunicipalScopeService municipalScopeService) {
        this.safeLocationRepository = safeLocationRepository;
        this.occupancyRepository = occupancyRepository;
        this.municipalityRepository = municipalityRepository;
        this.municipalScopeService = municipalScopeService;
    }

    @Transactional
    public SafeLocationResponseData createSafeLocation(CreateSafeLocationRequest request, UUID operatorId) {
        if (operatorId != null) {
            municipalScopeService.validateScope(operatorId, request.municipalityId());
        }

        municipalityRepository.findById(request.municipalityId())
                .orElseThrow(() -> new ResourceNotFoundException("Município", request.municipalityId()));

        Coordinates coords = Coordinates.of(request.latitude(), request.longitude());
        Point point = GeometryUtils.createPoint(coords.latitude(), coords.longitude());

        SafeLocation location = new SafeLocation(
                request.municipalityId(),
                request.name(),
                request.description(),
                point,
                request.address(),
                request.type() != null ? request.type() : SafeLocationType.SHELTER,
                request.capacity(),
                request.operationalStatus() != null ? request.operationalStatus() : SafeLocationOperationalStatus.OPEN,
                Boolean.TRUE.equals(request.accessibility()) ? "Acessibilidade completa para PCD" : "Sem informações de acessibilidade"
        );

        SafeLocation saved = safeLocationRepository.save(location);
        return toResponseData(saved, null);
    }

    @Transactional
    public SafeLocationResponseData updateAvailability(
            UUID locationId,
            UpdateSafeLocationAvailabilityRequest request,
            UUID operatorId
    ) {
        SafeLocation location = findById(locationId);

        if (operatorId != null) {
            municipalScopeService.validateScope(operatorId, location.getMunicipalityId());
        }

        if (request.operationalStatus() != null) {
            location.setOperationalStatus(request.operationalStatus());
        }

        Integer currentOccupancy = null;
        if (request.occupancy() != null) {
            if (location.getMaximumCapacity() != null && request.occupancy() > location.getMaximumCapacity() * 1.5) {
                throw new ValidationException("A ocupação informada excede de forma inconsistente a capacidade máxima do local.");
            }

            SafeLocationOccupancy occupancy = new SafeLocationOccupancy(
                    location,
                    request.occupancy(),
                    operatorId,
                    Instant.now()
            );
            occupancyRepository.save(occupancy);
            currentOccupancy = request.occupancy();
        } else {
            currentOccupancy = occupancyRepository.findFirstBySafeLocationIdOrderByReportedAtDesc(locationId)
                    .map(SafeLocationOccupancy::getOccupancyCount)
                    .orElse(null);
        }

        SafeLocation updated = safeLocationRepository.save(location);
        return toResponseData(updated, currentOccupancy);
    }

    @Transactional(readOnly = true)
    public Page<SafeLocationResponseData> listSafeLocations(
            UUID municipalityId,
            SafeLocationType type,
            Boolean accessibility,
            SafeLocationAvailability availability,
            Double latitude,
            Double longitude,
            Double radiusMeters,
            Pageable pageable
    ) {
        Specification<SafeLocation> spec = Specification.where(null);

        if (municipalityId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("municipalityId"), municipalityId));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("locationType"), type));
        }
        if (Boolean.TRUE.equals(accessibility)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("accessibilityInfo")), "%pcd%"));
        }

        Page<SafeLocation> pageResult = safeLocationRepository.findAll(spec, pageable);

        List<SafeLocationResponseData> results = new ArrayList<>();
        for (SafeLocation loc : pageResult.getContent()) {
            Integer occ = occupancyRepository.findFirstBySafeLocationIdOrderByReportedAtDesc(loc.getId())
                    .map(SafeLocationOccupancy::getOccupancyCount)
                    .orElse(null);

            SafeLocationResponseData data = toResponseData(loc, occ);

            if (availability != null && data.availability() != availability) {
                continue;
            }

            if (latitude != null && longitude != null && radiusMeters != null) {
                double distance = OccurrenceService.calculateDistanceMeters(
                        latitude, longitude,
                        loc.getLocation().getY(), loc.getLocation().getX()
                );
                if (distance > radiusMeters) {
                    continue;
                }
            }

            results.add(data);
        }

        return new PageImpl<>(results, pageable, results.size());
    }

    @Transactional(readOnly = true)
    public SafeLocationResponseData getSafeLocation(UUID locationId) {
        SafeLocation location = findById(locationId);
        Integer occ = occupancyRepository.findFirstBySafeLocationIdOrderByReportedAtDesc(locationId)
                .map(SafeLocationOccupancy::getOccupancyCount)
                .orElse(null);

        return toResponseData(location, occ);
    }

    private SafeLocation findById(UUID id) {
        return safeLocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ponto Seguro", id));
    }

    private SafeLocationResponseData toResponseData(SafeLocation loc, Integer occupancy) {
        SafeLocationAvailability availability = loc.computeAvailability(occupancy);

        boolean hasAccessibility = loc.getAccessibilityInfo() != null
                && loc.getAccessibilityInfo().toLowerCase().contains("pcd");

        String occupancyStatus = (occupancy != null) ? "CURRENT" : "UNKNOWN";

        return new SafeLocationResponseData(
                loc.getId(),
                loc.getMunicipalityId(),
                loc.getName(),
                loc.getDescription(),
                loc.getLocationType(),
                loc.getAddress(),
                new CoordinatesDto(loc.getLocation().getY(), loc.getLocation().getX()),
                loc.getMaximumCapacity(),
                occupancy,
                availability,
                hasAccessibility,
                loc.getOperationalStatus(),
                loc.getLastVerifiedAt(),
                occupancyStatus
        );
    }
}
