package br.com.guardian.accesscontrol.infrastructure;

import br.com.guardian.accesscontrol.domain.model.Permission;
import br.com.guardian.accesscontrol.domain.model.Role;
import br.com.guardian.accesscontrol.domain.repository.PermissionRepository;
import br.com.guardian.accesscontrol.domain.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(1)
public class AccessControlDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AccessControlDataInitializer.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public AccessControlDataInitializer(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            return;
        }

        log.info("Inicializando papéis e permissões padrão do Guardian...");

        Permission alertRead = getOrCreatePermission("ALERT_READ", "Consultar alertas de risco");
        Permission alertCreate = getOrCreatePermission("ALERT_CREATE", "Criar alertas de risco");
        Permission alertUpdate = getOrCreatePermission("ALERT_UPDATE", "Atualizar alertas existentes");
        Permission alertClose = getOrCreatePermission("ALERT_CLOSE", "Encerrar alertas ativos");

        Permission occurrenceRead = getOrCreatePermission("OCCURRENCE_READ", "Consultar relatos de ocorrências");
        Permission occurrenceCreate = getOrCreatePermission("OCCURRENCE_CREATE", "Registrar relatos de ocorrências");
        Permission occurrenceUpdate = getOrCreatePermission("OCCURRENCE_UPDATE", "Atualizar relatos e vincular a incidentes");

        Permission safeLocationRead = getOrCreatePermission("SAFE_LOCATION_READ", "Consultar pontos seguros");
        Permission safeLocationCreate = getOrCreatePermission("SAFE_LOCATION_CREATE", "Cadastrar pontos seguros");
        Permission safeLocationUpdate = getOrCreatePermission("SAFE_LOCATION_UPDATE", "Atualizar pontos seguros e ocupação");

        Permission userRead = getOrCreatePermission("USER_READ", "Consultar dados de usuários");
        Permission roleManage = getOrCreatePermission("ROLE_MANAGE", "Gerenciar papéis e acessos");

        // Role CITIZEN
        Role citizen = new Role("CITIZEN", "Cidadão usuário da plataforma");
        citizen.getPermissions().addAll(List.of(occurrenceCreate, alertRead, safeLocationRead));
        roleRepository.save(citizen);

        // Role MUNICIPAL_OPERATOR
        Role operator = new Role("MUNICIPAL_OPERATOR", "Operador municipal de defesa civil");
        operator.getPermissions().addAll(List.of(occurrenceRead, occurrenceUpdate, alertRead, alertCreate, safeLocationRead));
        roleRepository.save(operator);

        // Role MUNICIPAL_ADMIN
        Role municipalAdmin = new Role("MUNICIPAL_ADMIN", "Administrador municipal");
        municipalAdmin.getPermissions().addAll(List.of(
                userRead, occurrenceRead, occurrenceUpdate, alertRead, alertCreate, alertUpdate, alertClose,
                safeLocationRead, safeLocationCreate, safeLocationUpdate
        ));
        roleRepository.save(municipalAdmin);

        // Role GLOBAL_ADMIN
        Role globalAdmin = new Role("GLOBAL_ADMIN", "Administrador global da plataforma");
        globalAdmin.getPermissions().addAll(List.of(
                userRead, roleManage, occurrenceRead, occurrenceUpdate, alertRead, alertCreate, alertUpdate,
                alertClose, safeLocationRead, safeLocationCreate, safeLocationUpdate
        ));
        roleRepository.save(globalAdmin);

        log.info("Papéis e permissões padrão inicializados com sucesso.");
    }

    private Permission getOrCreatePermission(String name, String description) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(new Permission(name, description)));
    }
}
