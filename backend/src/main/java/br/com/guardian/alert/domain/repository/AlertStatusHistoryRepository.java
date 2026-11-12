package br.com.guardian.alert.domain.repository;

import br.com.guardian.alert.domain.model.AlertStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertStatusHistoryRepository extends JpaRepository<AlertStatusHistory, UUID> {

    List<AlertStatusHistory> findByAlertIdOrderByChangedAtDesc(UUID alertId);
}
