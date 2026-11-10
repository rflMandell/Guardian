package br.com.guardian.accesscontrol.application;

import br.com.guardian.accesscontrol.domain.model.MembershipStatus;
import br.com.guardian.accesscontrol.domain.model.Role;
import br.com.guardian.accesscontrol.domain.repository.MunicipalityMembershipRepository;
import br.com.guardian.shared.exception.AccessDeniedException;
import br.com.guardian.usermanagement.domain.model.AccountType;
import br.com.guardian.usermanagement.domain.model.User;
import br.com.guardian.usermanagement.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MunicipalScopeServiceTests {

    @Mock
    private MunicipalityMembershipRepository membershipRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MunicipalScopeService scopeService;

    private UUID userId;
    private UUID municipalityId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        municipalityId = UUID.randomUUID();
    }

    @Test
    @DisplayName("SCOPE-001: Administrador global deve ter acesso a qualquer município")
    void globalAdminShouldHaveAccessToAnyMunicipality() {
        User admin = new User("Admin", "admin@guardian.com", "hash", AccountType.ADMIN);
        admin.addRole(new Role("GLOBAL_ADMIN", "Global Admin"));

        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));

        assertTrue(scopeService.hasAccess(userId, municipalityId));
        assertDoesNotThrow(() -> scopeService.validateScope(userId, municipalityId));
    }

    @Test
    @DisplayName("SCOPE-002: Operador municipal com vínculo ativo deve ter acesso ao seu município")
    void operatorWithActiveMembershipShouldHaveAccess() {
        User operator = new User("Operador", "op@email.com", "hash", AccountType.ADMIN);
        operator.addRole(new Role("MUNICIPAL_OPERATOR", "Operator"));

        when(userRepository.findById(userId)).thenReturn(Optional.of(operator));
        when(membershipRepository.existsByUserIdAndMunicipalityIdAndStatus(userId, municipalityId, MembershipStatus.ACTIVE))
                .thenReturn(true);

        assertTrue(scopeService.hasAccess(userId, municipalityId));
        assertDoesNotThrow(() -> scopeService.validateScope(userId, municipalityId));
    }

    @Test
    @DisplayName("SCOPE-003: Operador sem vínculo no município deve ter acesso negado (Cross-Municipality)")
    void operatorWithoutMembershipShouldBeDeniedAccess() {
        User operator = new User("Operador", "op@email.com", "hash", AccountType.ADMIN);
        operator.addRole(new Role("MUNICIPAL_OPERATOR", "Operator"));

        when(userRepository.findById(userId)).thenReturn(Optional.of(operator));
        when(membershipRepository.existsByUserIdAndMunicipalityIdAndStatus(userId, municipalityId, MembershipStatus.ACTIVE))
                .thenReturn(false);

        assertFalse(scopeService.hasAccess(userId, municipalityId));
        assertThrows(AccessDeniedException.class, () -> scopeService.validateScope(userId, municipalityId));
    }
}
