package br.com.guardian.safelocation.domain.repository;

import br.com.guardian.safelocation.domain.model.SafeLocationOccupancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SafeLocationOccupancyRepository extends JpaRepository<SafeLocationOccupancy, UUID> {

    Optional<SafeLocationOccupancy> findFirstBySafeLocationIdOrderByReportedAtDesc(UUID safeLocationId);
}
