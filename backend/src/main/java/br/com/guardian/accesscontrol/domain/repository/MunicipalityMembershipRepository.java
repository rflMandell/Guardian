package br.com.guardian.accesscontrol.domain.repository;

import br.com.guardian.accesscontrol.domain.model.MembershipStatus;
import br.com.guardian.accesscontrol.domain.model.MunicipalityMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MunicipalityMembershipRepository extends JpaRepository<MunicipalityMembership, UUID> {
    List<MunicipalityMembership> findByUserIdAndStatus(UUID userId, MembershipStatus status);
    Optional<MunicipalityMembership> findByUserIdAndMunicipalityId(UUID userId, UUID municipalityId);
    boolean existsByUserIdAndMunicipalityIdAndStatus(UUID userId, UUID municipalityId, MembershipStatus status);
}
