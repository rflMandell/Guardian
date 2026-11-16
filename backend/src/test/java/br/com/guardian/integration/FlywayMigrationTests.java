package br.com.guardian.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validação de Migrações Flyway (Etapa 10 / TEST_PLAN DB-001).
 * Assegura que todas as 14 migrations versionadas estão catalogadas e íntegras.
 */
class FlywayMigrationTests {

    @Test
    @DisplayName("DB-001 & DB-003: Validar que todas as 14 migrations Flyway estão catalogadas e versionadas sequencialmente")
    void shouldVerifyAllFlywayMigrationsPresentAndOrdered() {
        Flyway flyway = Flyway.configure()
                .locations("classpath:db/migration")
                .dataSource("jdbc:h2:mem:flyway_catalog_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "")
                .load();

        MigrationInfo[] migrations = flyway.info().all();
        assertThat(migrations)
                .as("Deverá conter no mínimo 14 migrations versionadas do banco de dados")
                .hasSizeGreaterThanOrEqualTo(14);

        for (int i = 0; i < 14; i++) {
            assertThat(migrations[i].getVersion().getVersion())
                    .isEqualTo(String.valueOf(i + 1));
        }
    }
}
