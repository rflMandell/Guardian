package br.com.guardian.geolocation.domain.repository;

import br.com.guardian.geolocation.domain.model.RiskArea;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RiskAreaRepository extends JpaRepository<RiskArea, UUID> {

    List<RiskArea> findByMunicipalityIdAndActiveTrue(UUID municipalityId);

    List<RiskArea> findByMunicipalityIdAndVulnerabilityLevelAndActiveTrue(UUID municipalityId, String vulnerabilityLevel);

    @Query("SELECT ra FROM RiskArea ra WHERE ra.active = true AND contains(ra.geometry, :point) = true")
    List<RiskArea> findContainingPoint(@Param("point") Point point);
}
