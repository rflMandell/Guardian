package br.com.guardian.risk.domain.repository;

import br.com.guardian.occurrence.domain.model.EventCategory;
import br.com.guardian.risk.domain.model.RiskAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID>, JpaSpecificationExecutor<RiskAssessment> {

    Page<RiskAssessment> findByMunicipalityId(UUID municipalityId, Pageable pageable);

    Optional<RiskAssessment> findFirstByMunicipalityIdAndEventTypeOrderByEvaluatedAtDesc(UUID municipalityId, EventCategory eventType);

    Optional<RiskAssessment> findFirstByRiskAreaIdOrderByEvaluatedAtDesc(UUID riskAreaId);

    Optional<RiskAssessment> findFirstByMunicipalityIdOrderByEvaluatedAtDesc(UUID municipalityId);
}
