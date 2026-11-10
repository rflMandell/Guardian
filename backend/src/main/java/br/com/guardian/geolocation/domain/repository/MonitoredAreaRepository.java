package br.com.guardian.geolocation.domain.repository;

import br.com.guardian.geolocation.domain.model.MonitoredArea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MonitoredAreaRepository extends JpaRepository<MonitoredArea, UUID> {

    List<MonitoredArea> findByMunicipalityIdAndActiveTrue(UUID municipalityId);

    Page<MonitoredArea> findByMunicipalityId(UUID municipalityId, Pageable pageable);
}
