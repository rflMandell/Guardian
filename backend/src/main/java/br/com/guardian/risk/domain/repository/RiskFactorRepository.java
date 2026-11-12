package br.com.guardian.risk.domain.repository;

import br.com.guardian.risk.domain.model.RiskFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RiskFactorRepository extends JpaRepository<RiskFactor, UUID> {

    List<RiskFactor> findByRiskAssessmentId(UUID riskAssessmentId);
}
