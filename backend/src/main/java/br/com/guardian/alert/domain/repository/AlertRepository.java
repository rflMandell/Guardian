package br.com.guardian.alert.domain.repository;

import br.com.guardian.alert.domain.model.Alert;
import br.com.guardian.alert.domain.model.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID>, JpaSpecificationExecutor<Alert> {

    Page<Alert> findByMunicipalityId(UUID municipalityId, Pageable pageable);

    List<Alert> findByMunicipalityIdAndStatus(UUID municipalityId, AlertStatus status);
}
