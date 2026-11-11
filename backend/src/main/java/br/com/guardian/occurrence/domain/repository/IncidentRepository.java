package br.com.guardian.occurrence.domain.repository;

import br.com.guardian.occurrence.domain.model.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Page<Incident> findByMunicipalityId(UUID municipalityId, Pageable pageable);

    List<Incident> findByMunicipalityIdAndStatusNot(UUID municipalityId, String status);
}
