package br.com.guardian.alert.domain.repository;

import br.com.guardian.alert.domain.model.AlertArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertAreaRepository extends JpaRepository<AlertArea, UUID> {

    List<AlertArea> findByAlertId(UUID alertId);
}
