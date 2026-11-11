package br.com.guardian.occurrence.domain.repository;

import br.com.guardian.occurrence.domain.model.EventCategory;
import br.com.guardian.occurrence.domain.model.OccurrenceReport;
import br.com.guardian.occurrence.domain.model.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OccurrenceReportRepository extends JpaRepository<OccurrenceReport, UUID>, JpaSpecificationExecutor<OccurrenceReport> {

    Page<OccurrenceReport> findByMunicipalityId(UUID municipalityId, Pageable pageable);

    @Query("SELECT r FROM OccurrenceReport r WHERE r.municipalityId = :municipalityId " +
            "AND r.eventType = :eventType " +
            "AND r.status NOT IN ('DISMISSED', 'CLOSED') " +
            "AND r.reportedAt >= :since")
    List<OccurrenceReport> findRecentReportsByMunicipalityAndType(
            @Param("municipalityId") UUID municipalityId,
            @Param("eventType") EventCategory eventType,
            @Param("since") Instant since
    );
}
