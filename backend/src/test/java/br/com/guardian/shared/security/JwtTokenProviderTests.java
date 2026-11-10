package br.com.guardian.shared.security;

import br.com.guardian.accesscontrol.domain.model.Permission;
import br.com.guardian.accesscontrol.domain.model.Role;
import br.com.guardian.usermanagement.domain.model.AccountType;
import br.com.guardian.usermanagement.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTests {

    private JwtTokenProvider tokenProvider;
    private final String secret = "test_super_secret_local_key_minimum_256_bits_length_12345";

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(secret, 15);
    }

    @Test
    @DisplayName("JWT-001: Deve gerar access token válido com claims de identificação e permissões")
    void shouldGenerateValidAccessToken() {
        UUID userId = UUID.randomUUID();
        User user = new User("João Silva", "joao@email.com", "hash", AccountType.CITIZEN);
        user.setId(userId);

        Role citizenRole = new Role("CITIZEN", "Cidadão");
        citizenRole.addPermission(new Permission("ALERT_READ", "Ler alertas"));
        user.addRole(citizenRole);

        String token = tokenProvider.generateAccessToken(user);

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals(userId, tokenProvider.getUserIdFromToken(token));
        assertEquals("joao@email.com", tokenProvider.getEmailFromToken(token));
        assertEquals(900, tokenProvider.getExpirationSeconds());
    }

    @Test
    @DisplayName("JWT-002: Deve rejeitar token adulterado ou inválido")
    void shouldRejectInvalidToken() {
        assertFalse(tokenProvider.validateToken("token.invalido.adulterado"));
        assertFalse(tokenProvider.validateToken(""));
        assertFalse(tokenProvider.validateToken(null));
    }
}
