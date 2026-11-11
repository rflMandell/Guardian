package br.com.guardian.occurrence.domain.repository;

import br.com.guardian.occurrence.domain.model.IncidentReportLink;
import br.com.guardian.occurrence.domain.model.IncidentReportLinkId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentReportLinkRepository extends JpaRepository<IncidentReportLink, IncidentReportLinkId> {

    boolean existsByReportId(UUID reportId);

    Optional<IncidentReportLink> findByReportId(UUID reportId);

    List<IncidentReportLink> findByIncidentId(UUID incidentId);
}
