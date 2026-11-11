package br.com.guardian.occurrence.application;

import br.com.guardian.accesscontrol.application.MunicipalScopeService;
import br.com.guardian.geolocation.util.Coordinates;
import br.com.guardian.geolocation.util.GeometryUtils;
import br.com.guardian.municipality.domain.model.Municipality;
import br.com.guardian.municipality.domain.repository.MunicipalityRepository;
import br.com.guardian.occurrence.api.dto.*;
import br.com.guardian.occurrence.domain.model.*;
import br.com.guardian.occurrence.domain.repository.IncidentReportLinkRepository;
import br.com.guardian.occurrence.domain.repository.IncidentRepository;
import br.com.guardian.occurrence.domain.repository.OccurrenceReportRepository;
import br.com.guardian.shared.exception.BusinessException;
import br.com.guardian.shared.exception.ResourceNotFoundException;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OccurrenceService {

    private static final Logger log = LoggerFactory.getLogger(OccurrenceService.class);
    private static final double DUPLICATE_DISTANCE_METERS = 500.0;
    private static final Duration DUPLICATE_TIME_WINDOW = Duration.ofHours(4);

    private final OccurrenceReportRepository reportRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentReportLinkRepository linkRepository;
    private final MunicipalityRepository municipalityRepository;
    private final MunicipalScopeService municipalScopeService;

    public OccurrenceService(OccurrenceReportRepository reportRepository,
                             IncidentRepository incidentRepository,
                             IncidentReportLinkRepository linkRepository,
                             MunicipalityRepository municipalityRepository,
                             MunicipalScopeService municipalScopeService) {
        this.reportRepository = reportRepository;
        this.incidentRepository = incidentRepository;
        this.linkRepository = linkRepository;
        this.municipalityRepository = municipalityRepository;
        this.municipalScopeService = municipalScopeService;
    }

    @Transactional
    public OccurrenceResponseData createReport(CreateOccurrenceRequest request, UUID authenticatedUserId) {
        // Validação estrita de coordenadas geográficas
        Coordinates coords = Coordinates.of(request.location().latitude(), request.location().longitude());
        Point point = GeometryUtils.createPoint(coords.latitude(), coords.longitude());

        // Identificação geográfica do município
        List<Municipality> matching = municipalityRepository.findByContainingPoint(point);
        Municipality municipality = matching.isEmpty()
                ? municipalityRepository.findAll().stream().findFirst().orElse(null)
                : matching.get(0);

        if (municipality == null) {
            throw new BusinessException("MUNICIPALITY_NOT_COVERED",
                    "A localização informada não pertence a um município monitorado.");
        }

        // RN012: Relatos anônimos ou sem autenticação não armazenam identificador do usuário
        boolean isAnonymous = Boolean.TRUE.equals(request.anonymous()) || authenticatedUserId == null;
        UUID authorId = isAnonymous ? null : authenticatedUserId;

        Instant reportedTime = request.observedAt() != null ? request.observedAt() : Instant.now();

        OccurrenceReport report = new OccurrenceReport(
                authorId,
                municipality.getId(),
                request.eventCategory(),
                request.description(),
                point,
                reportedTime
        );

        // RN014: Verificação de potenciais duplicidades no raio de 500m nas últimas 4 horas
        int duplicateCount = countPotentialDuplicates(municipality.getId(), request.eventCategory(), point, reportedTime);
        if (duplicateCount > 0) {
            log.info("Relato potencialmente duplicado detectado: {} relatos similares próximos encontrados.", duplicateCount);
        }

        OccurrenceReport saved = reportRepository.save(report);

        return new OccurrenceResponseData(
                saved.getId(),
                saved.getEventType(),
                saved.getStatus(),
                saved.isAnonymous(),
                saved.getMunicipalityId(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public Page<OccurrenceSummaryData> listReports(UUID municipalityId,
                                                   EventCategory eventCategory,
                                                   ReportStatus status,
                                                   Double latitude,
                                                   Double longitude,
                                                   Double radiusMeters,
                                                   Instant from,
                                                   Instant to,
                                                   Pageable pageable) {
        Specification<OccurrenceReport> spec = Specification.where(null);

        if (municipalityId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("municipalityId"), municipalityId));
        }
        if (eventCategory != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("eventType"), eventCategory));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("reportedAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("reportedAt"), to));
        }

        Page<OccurrenceReport> pageResult = reportRepository.findAll(spec, pageable);

        // Se filtro de raio informado, refina a lista
        if (latitude != null && longitude != null && radiusMeters != null) {
            List<OccurrenceSummaryData> filtered = new ArrayList<>();
            for (OccurrenceReport r : pageResult.getContent()) {
                double distance = calculateDistanceMeters(
                        latitude, longitude,
                        r.getLocation().getY(), r.getLocation().getX()
                );
                if (distance <= radiusMeters) {
                    filtered.add(toSummary(r));
                }
            }
            return new PageImpl<>(filtered, pageable, filtered.size());
        }

        return pageResult.map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public OccurrenceDetailData getReport(UUID reportId) {
        OccurrenceReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Relato de ocorrência", reportId));

        int duplicates = countPotentialDuplicates(
                report.getMunicipalityId(),
                report.getEventType(),
                report.getLocation(),
                report.getReportedAt()
        );

        return toDetail(report, duplicates);
    }

    @Transactional
    public OccurrenceDetailData updateStatus(UUID reportId, UpdateOccurrenceStatusRequest request, UUID operatorId) {
        OccurrenceReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Relato de ocorrência", reportId));

        // Validação de escopo municipal (RN001 / RBAC-002)
        if (operatorId != null) {
            municipalScopeService.validateScope(operatorId, report.getMunicipalityId());
        }

        report.setStatus(request.status());
        OccurrenceReport updated = reportRepository.save(report);

        int duplicates = countPotentialDuplicates(
                updated.getMunicipalityId(),
                updated.getEventType(),
                updated.getLocation(),
                updated.getReportedAt()
        );

        return toDetail(updated, duplicates);
    }

    @Transactional
    public void linkToIncident(UUID reportId, UUID incidentId, UUID operatorId) {
        OccurrenceReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Relato de ocorrência", reportId));

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incidente", incidentId));

        // Validação de escopo municipal
        if (operatorId != null) {
            municipalScopeService.validateScope(operatorId, report.getMunicipalityId());
        }

        if (!report.getMunicipalityId().equals(incident.getMunicipalityId())) {
            throw new BusinessException("MUNICIPALITY_MISMATCH",
                    "O relato e o incidente pertencem a municípios distintos.");
        }

        if (linkRepository.existsByReportId(reportId)) {
            throw new BusinessException("REPORT_ALREADY_LINKED",
                    "Este relato já se encontra vinculado a um incidente.");
        }

        IncidentReportLink link = new IncidentReportLink(incidentId, reportId, "MANUAL");
        linkRepository.save(link);

        report.setStatus(ReportStatus.LINKED_TO_INCIDENT);
        reportRepository.save(report);
    }

    private int countPotentialDuplicates(UUID municipalityId, EventCategory category, Point point, Instant referenceTime) {
        Instant since = referenceTime.minus(DUPLICATE_TIME_WINDOW);
        List<OccurrenceReport> recent = reportRepository.findRecentReportsByMunicipalityAndType(municipalityId, category, since);

        int duplicates = 0;
        for (OccurrenceReport other : recent) {
            double distance = calculateDistanceMeters(
                    point.getY(), point.getX(),
                    other.getLocation().getY(), other.getLocation().getX()
            );
            if (distance <= DUPLICATE_DISTANCE_METERS) {
                duplicates++;
            }
        }
        return duplicates;
    }

    public static double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private OccurrenceSummaryData toSummary(OccurrenceReport r) {
        return new OccurrenceSummaryData(
                r.getId(),
                r.getEventType(),
                r.getStatus(),
                r.isAnonymous(),
                r.getMunicipalityId(),
                r.getLocation().getY(),
                r.getLocation().getX(),
                r.getReportedAt()
        );
    }

    private OccurrenceDetailData toDetail(OccurrenceReport r, int duplicateCount) {
        return new OccurrenceDetailData(
                r.getId(),
                r.getEventType(),
                r.getDescription(),
                new CoordinatesDto(r.getLocation().getY(), r.getLocation().getX()),
                r.getStatus(),
                r.isAnonymous(),
                r.getMunicipalityId(),
                r.getReportedAt(),
                r.getCreatedAt(),
                duplicateCount
        );
    }
}
