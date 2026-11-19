package br.com.encostasegura.usermanagement.domain.repository;

import br.com.encostasegura.usermanagement.domain.model.CitizenProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CitizenProfileRepository extends JpaRepository<CitizenProfile, UUID> {
    Optional<CitizenProfile> findByUserId(UUID userId);
}
