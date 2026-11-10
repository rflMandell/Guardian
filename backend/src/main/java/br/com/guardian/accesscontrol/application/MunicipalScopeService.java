package br.com.guardian.accesscontrol.application;

import br.com.guardian.accesscontrol.domain.model.MembershipStatus;
import br.com.guardian.accesscontrol.domain.repository.MunicipalityMembershipRepository;
import br.com.guardian.shared.exception.AccessDeniedException;
import br.com.guardian.usermanagement.domain.model.User;
import br.com.guardian.usermanagement.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MunicipalScopeService {

    private final MunicipalityMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public MunicipalScopeService(MunicipalityMembershipRepository membershipRepository, UserRepository userRepository) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public boolean hasAccess(UUID userId, UUID municipalityId) {
        if (userId == null || municipalityId == null) {
            return false;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        // Administradores globais têm acesso a qualquer município
        boolean isGlobalAdmin = user.getRoles().stream()
                .anyMatch(r -> "GLOBAL_ADMIN".equals(r.getName()));
        if (isGlobalAdmin) {
            return true;
        }

        // Operadores e administradores municipais dependem de vínculo ativo
        return membershipRepository.existsByUserIdAndMunicipalityIdAndStatus(userId, municipalityId, MembershipStatus.ACTIVE);
    }

    public void validateScope(UUID userId, UUID municipalityId) {
        if (!hasAccess(userId, municipalityId)) {
            throw new AccessDeniedException(
                    String.format("Usuário '%s' não possui vínculo ativo nem permissão para operar no município '%s'.", userId, municipalityId)
            );
        }
    }
}
