package br.com.guardian.safelocation.domain.repository;

import br.com.guardian.safelocation.domain.model.SafeLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SafeLocationRepository extends JpaRepository<SafeLocation, UUID>, JpaSpecificationExecutor<SafeLocation> {

    Page<SafeLocation> findByMunicipalityId(UUID municipalityId, Pageable pageable);
}
