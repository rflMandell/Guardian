package br.com.guardian.shared.config;

import br.com.guardian.accesscontrol.domain.model.MembershipStatus;
import br.com.guardian.accesscontrol.domain.model.MunicipalityMembership;
import br.com.guardian.accesscontrol.domain.model.Role;
import br.com.guardian.accesscontrol.domain.repository.MunicipalityMembershipRepository;
import br.com.guardian.accesscontrol.domain.repository.RoleRepository;
import br.com.guardian.geolocation.util.GeometryUtils;
import br.com.guardian.municipality.domain.model.Municipality;
import br.com.guardian.municipality.domain.repository.MunicipalityRepository;
import br.com.guardian.safelocation.domain.model.SafeLocation;
import br.com.guardian.safelocation.domain.model.SafeLocationOccupancy;
import br.com.guardian.safelocation.domain.model.SafeLocationOperationalStatus;
import br.com.guardian.safelocation.domain.model.SafeLocationType;
import br.com.guardian.safelocation.domain.repository.SafeLocationOccupancyRepository;
import br.com.guardian.safelocation.domain.repository.SafeLocationRepository;
import br.com.guardian.usermanagement.domain.model.AccountType;
import br.com.guardian.usermanagement.domain.model.User;
import br.com.guardian.usermanagement.domain.repository.UserRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Inicializador de dados de demonstração (DEMO_SCRIPT / MVP).
 * Garante que a aplicação inicie pronta para demonstração e testes integrados,
 * sem depender de manipulação manual do banco de dados (Critério da Etapa 9).
 */
@Component
@Order(2)
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final MunicipalityRepository municipalityRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MunicipalityMembershipRepository membershipRepository;
    private final SafeLocationRepository safeLocationRepository;
    private final SafeLocationOccupancyRepository occupancyRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(MunicipalityRepository municipalityRepository,
                               UserRepository userRepository,
                               RoleRepository roleRepository,
                               MunicipalityMembershipRepository membershipRepository,
                               SafeLocationRepository safeLocationRepository,
                               SafeLocationOccupancyRepository occupancyRepository,
                               PasswordEncoder passwordEncoder) {
        this.municipalityRepository = municipalityRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.membershipRepository = membershipRepository;
        this.safeLocationRepository = safeLocationRepository;
        this.occupancyRepository = occupancyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Verificando e carregando dados de demonstração do Guardian...");

        // 1. Município de Demonstração Principal (São Paulo)
        Municipality demoMun = municipalityRepository.findByIbgeCode("3550308")
                .orElseGet(() -> {
                    Municipality m = new Municipality("São Paulo", "SP", "3550308", "BR");
                    Coordinate[] spCoords = new Coordinate[] {
                            new Coordinate(-47.0, -24.1),
                            new Coordinate(-46.2, -24.1),
                            new Coordinate(-46.2, -23.3),
                            new Coordinate(-47.0, -23.3),
                            new Coordinate(-47.0, -24.1)
                    };
                    Polygon poly = GeometryUtils.getFactory().createPolygon(spCoords);
                    m.setBoundary(GeometryUtils.createMultiPolygon(poly));
                    return municipalityRepository.save(m);
                });

        // 2. Município Secundário para testes de Isolamento Municipal (Campinas)
        Municipality campinasMun = municipalityRepository.findByIbgeCode("3509502")
                .orElseGet(() -> {
                    Municipality m = new Municipality("Campinas", "SP", "3509502", "BR");
                    Coordinate[] campCoords = new Coordinate[] {
                            new Coordinate(-47.3, -23.1),
                            new Coordinate(-46.9, -23.1),
                            new Coordinate(-46.9, -22.7),
                            new Coordinate(-47.3, -22.7),
                            new Coordinate(-47.3, -23.1)
                    };
                    Polygon poly = GeometryUtils.getFactory().createPolygon(campCoords);
                    m.setBoundary(GeometryUtils.createMultiPolygon(poly));
                    return municipalityRepository.save(m);
                });

        // 3. Usuários de Demonstração (DEMO_SCRIPT / TEST_PLAN)
        String defaultPasswordHash = passwordEncoder.encode("SenhaSegura@123");

        // 3.1 Cidadão (cidadao.demo@example.com)
        if (userRepository.findByEmail("cidadao.demo@example.com").isEmpty()) {
            User citizen = new User("Cidadão de Demonstração", "cidadao.demo@example.com", defaultPasswordHash, AccountType.CITIZEN);
            citizen.setEmailVerified(true);
            Optional<Role> roleOpt = roleRepository.findByName("CITIZEN");
            roleOpt.ifPresent(r -> citizen.setRoles(Set.of(r)));
            userRepository.save(citizen);
        }

        // 3.2 Operador Municipal (operador.demo@example.com) vinculado a São Paulo
        User operator = userRepository.findByEmail("operador.demo@example.com").orElseGet(() -> {
            User op = new User("Operador Municipal Demo", "operador.demo@example.com", defaultPasswordHash, AccountType.ADMIN);
            op.setEmailVerified(true);
            Optional<Role> roleOpt = roleRepository.findByName("MUNICIPAL_OPERATOR");
            roleOpt.ifPresent(r -> op.setRoles(Set.of(r)));
            return userRepository.save(op);
        });

        // Garante o vínculo municipal do operador
        if (!membershipRepository.existsByUserIdAndMunicipalityIdAndStatus(operator.getId(), demoMun.getId(), MembershipStatus.ACTIVE)) {
            membershipRepository.save(new MunicipalityMembership(operator, demoMun.getId(), MembershipStatus.ACTIVE));
        }

        // 3.3 Administrador Global (admin.demo@example.com)
        if (userRepository.findByEmail("admin.demo@example.com").isEmpty()) {
            User admin = new User("Administrador Global Demo", "admin.demo@example.com", defaultPasswordHash, AccountType.ADMIN);
            admin.setEmailVerified(true);
            Optional<Role> roleOpt = roleRepository.findByName("GLOBAL_ADMIN");
            roleOpt.ifPresent(r -> admin.setRoles(Set.of(r)));
            userRepository.save(admin);
        }

        // 4. Pontos Seguros Iniciais (Acolhimento e Abrigo)
        if (safeLocationRepository.count() == 0) {
            SafeLocation shelter = new SafeLocation(
                    demoMun.getId(),
                    "Ginásio Poliesportivo do Ibirapuera",
                    "Ponto seguro equipado para acolhimento de famílias em áreas de alagamento.",
                    GeometryUtils.createPoint(-23.5874, -46.6576),
                    "Rua Manuel da Nóbrega, 1361, Ibirapuera",
                    SafeLocationType.GYMNASIUM,
                    300,
                    SafeLocationOperationalStatus.OPEN,
                    "Acessibilidade completa com rampas e sanitários adaptados"
            );
            SafeLocation savedShelter = safeLocationRepository.save(shelter);

            // Registra ocupação atual de 45 pessoas (Disponível)
            SafeLocationOccupancy occ = new SafeLocationOccupancy(savedShelter, 45, null, Instant.now());
            occupancyRepository.save(occ);

            // Segundo abrigo de demonstração: Unidade Escolar (com lotação cheia para testar RN042)
            SafeLocation schoolShelter = new SafeLocation(
                    demoMun.getId(),
                    "Escola Municipal de Acolhimento Sé",
                    "Abrigo comunitário temporário emergencial.",
                    GeometryUtils.createPoint(-23.5505, -46.6333),
                    "Praça da Sé, s/n, Centro",
                    SafeLocationType.SCHOOL,
                    150,
                    SafeLocationOperationalStatus.FULL,
                    "Rampas de acesso"
            );
            SafeLocation savedSchool = safeLocationRepository.save(schoolShelter);
            SafeLocationOccupancy fullOcc = new SafeLocationOccupancy(savedSchool, 150, null, Instant.now());
            occupancyRepository.save(fullOcc);
        }

        log.info("Carga inicial de dados de demonstração concluída com sucesso para '{}' e '{}'.", demoMun.getName(), campinasMun.getName());
    }
}
