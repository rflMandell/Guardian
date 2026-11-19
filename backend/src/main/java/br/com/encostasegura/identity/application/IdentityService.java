package br.com.encostasegura.identity.application;

import br.com.encostasegura.accesscontrol.domain.model.MembershipStatus;
import br.com.encostasegura.accesscontrol.domain.model.MunicipalityMembership;
import br.com.encostasegura.accesscontrol.domain.model.Permission;
import br.com.encostasegura.accesscontrol.domain.model.Role;
import br.com.encostasegura.accesscontrol.domain.repository.MunicipalityMembershipRepository;
import br.com.encostasegura.accesscontrol.domain.repository.RoleRepository;
import br.com.encostasegura.identity.api.dto.*;
import br.com.encostasegura.identity.domain.model.RefreshToken;
import br.com.encostasegura.identity.domain.repository.RefreshTokenRepository;
import br.com.encostasegura.shared.exception.BusinessException;
import br.com.encostasegura.shared.security.JwtTokenProvider;
import br.com.encostasegura.usermanagement.domain.model.AccountType;
import br.com.encostasegura.usermanagement.domain.model.CitizenProfile;
import br.com.encostasegura.usermanagement.domain.model.User;
import br.com.encostasegura.usermanagement.domain.model.UserStatus;
import br.com.encostasegura.usermanagement.domain.repository.CitizenProfileRepository;
import br.com.encostasegura.usermanagement.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class IdentityService {

    private final UserRepository userRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MunicipalityMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public IdentityService(UserRepository userRepository,
                           CitizenProfileRepository citizenProfileRepository,
                           RoleRepository roleRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           MunicipalityMembershipRepository membershipRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.citizenProfileRepository = citizenProfileRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public RegisterResponseData register(RegisterRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("EMAIL_ALREADY_REGISTERED", "O e-mail informado já está cadastrado.");
        }

        User user = new User(
                request.name().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                AccountType.CITIZEN
        );

        // Atribui papel CITIZEN
        Role citizenRole = roleRepository.findByName("CITIZEN")
                .orElseGet(() -> roleRepository.save(new Role("CITIZEN", "Cidadão")));
        user.addRole(citizenRole);

        User savedUser = userRepository.save(user);

        // Cria perfil de cidadão
        CitizenProfile profile = new CitizenProfile(savedUser, null);
        citizenProfileRepository.save(profile);

        return new RegisterResponseData(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getAccountType().name(),
                savedUser.isEmailVerified()
        );
    }

    @Transactional
    public LoginResponseData login(LoginRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "E-mail ou senha incorretos."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CREDENTIALS", "E-mail ou senha incorretos.");
        }

        if (user.getStatus() == UserStatus.BLOCKED || user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException("ACCOUNT_INACTIVE", "Esta conta encontra-se inativa ou bloqueada.");
        }

        String accessToken = tokenProvider.generateAccessToken(user);
        String rawRefreshToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID();
        String tokenHash = hashToken(rawRefreshToken);

        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        RefreshToken refreshToken = new RefreshToken(user, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        String primaryRole = user.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse("CITIZEN");

        List<String> permissions = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getName)
                .distinct()
                .toList();

        UUID municipalityId = membershipRepository.findByUserIdAndStatus(user.getId(), MembershipStatus.ACTIVE)
                .stream()
                .map(MunicipalityMembership::getMunicipalityId)
                .findFirst()
                .orElseGet(() -> citizenProfileRepository.findByUserId(user.getId())
                        .map(CitizenProfile::getMunicipalityId)
                        .orElse(null));

        LoginUserData userData = new LoginUserData(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                primaryRole,
                user.getRoles().stream().map(Role::getName).toList(),
                municipalityId,
                permissions
        );

        return new LoginResponseData(
                accessToken,
                "Bearer",
                tokenProvider.getExpirationSeconds(),
                rawRefreshToken,
                userData
        );
    }

    @Transactional
    public TokenRefreshResponseData refreshToken(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.refreshToken());

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN", "Refresh token inválido ou não encontrado."));

        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            throw new BusinessException("REVOKED_REFRESH_TOKEN", "O refresh token informado expirou ou foi revogado.");
        }

        // Rotaciona o refresh token: revoga o atual
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();
        String newAccessToken = tokenProvider.generateAccessToken(user);

        // Emite novo refresh token
        String newRawRefreshToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID();
        RefreshToken newRefreshToken = new RefreshToken(
                user,
                hashToken(newRawRefreshToken),
                Instant.now().plus(7, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(newRefreshToken);

        return new TokenRefreshResponseData(
                newAccessToken,
                "Bearer",
                tokenProvider.getExpirationSeconds(),
                newRawRefreshToken
        );
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String tokenHash = hashToken(request.refreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 não disponível", e);
        }
    }
}
