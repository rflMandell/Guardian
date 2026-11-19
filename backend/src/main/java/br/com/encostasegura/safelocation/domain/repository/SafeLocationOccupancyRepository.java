package br.com.encostasegura.safelocation.domain.repository;

import br.com.encostasegura.safelocation.domain.model.SafeLocationOccupancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SafeLocationOccupancyRepository extends JpaRepository<SafeLocationOccupancy, UUID> {

    Optional<SafeLocationOccupancy> findFirstBySafeLocationIdOrderByReportedAtDesc(UUID safeLocationId);
}
