package br.com.guardian.shared.security;

import br.com.guardian.accesscontrol.domain.model.Permission;
import br.com.guardian.accesscontrol.domain.model.Role;
import br.com.guardian.usermanagement.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtTokenProvider(
            @Value("${guardian.jwt.secret:guardian_super_secret_local_key_minimum_256_bits_length_12345}") String secret,
            @Value("${guardian.jwt.expiration-minutes:15}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        List<String> permissions = user.getRoles().stream()
                .map(Role::getPermissions)
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .map(Permission::getName)
                .distinct()
                .toList();

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getFullName())
                .claim("accountType", user.getAccountType().name())
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Token JWT inválido ou expirado: {}", ex.getMessage());
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserIdFromToken(String token) {
        String sub = getClaims(token).getSubject();
        return UUID.fromString(sub);
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).get("email", String.class);
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }
}
