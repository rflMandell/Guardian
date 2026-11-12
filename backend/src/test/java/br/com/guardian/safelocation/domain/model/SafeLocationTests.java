package br.com.guardian.safelocation.domain.model;

import br.com.guardian.geolocation.util.GeometryUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SafeLocation - Testes unitários do modelo e regras de disponibilidade")
class SafeLocationTests {

    @Test
    @DisplayName("RN042: Deve calcular como AVAILABLE quando o local estiver aberto e a ocupação for inferior à capacidade")
    void shouldComputeAvailabilityAsAvailable() {
        Point point = GeometryUtils.createPoint(-22.50, -43.18);
        SafeLocation location = new SafeLocation(
                UUID.randomUUID(), "Abrigo Central", "Abrigo", point,
                "Rua Principal, 100", SafeLocationType.SHELTER, 200,
                SafeLocationOperationalStatus.OPEN, "Acessibilidade PCD"
        );

        SafeLocationAvailability availability = location.computeAvailability(50);

        assertThat(availability).isEqualTo(SafeLocationAvailability.AVAILABLE);
    }

    @Test
    @DisplayName("RN042 / RN043: Deve calcular como FULL quando a ocupação atingir ou superar a capacidade máxima")
    void shouldComputeAvailabilityAsFullWhenCapacityReached() {
        Point point = GeometryUtils.createPoint(-22.50, -43.18);
        SafeLocation location = new SafeLocation(
                UUID.randomUUID(), "Ginásio Municipal", "Ginásio", point,
                "Av. dos Esportes, 50", SafeLocationType.GYMNASIUM, 100,
                SafeLocationOperationalStatus.OPEN, null
        );

        SafeLocationAvailability availability = location.computeAvailability(100);

        assertThat(availability).isEqualTo(SafeLocationAvailability.FULL);
    }

    @Test
    @DisplayName("RN042: Um local fechado ou interditado deve ser classificado como UNAVAILABLE")
    void shouldComputeAvailabilityAsUnavailableWhenClosed() {
        Point point = GeometryUtils.createPoint(-22.50, -43.18);
        SafeLocation location = new SafeLocation(
                UUID.randomUUID(), "Escola Interditada", "Escola", point,
                "Rua das Flores, 20", SafeLocationType.SCHOOL, 150,
                SafeLocationOperationalStatus.CLOSED, null
        );

        SafeLocationAvailability availability = location.computeAvailability(0);

        assertThat(availability).isEqualTo(SafeLocationAvailability.UNAVAILABLE);
    }

    @Test
    @DisplayName("RN042 e Regra de Segurança: Local com ocupação desconhecida NUNCA deve ser apresentado como AVAILABLE")
    void shouldComputeAvailabilityAsUnknownWhenOccupancyMissing() {
        Point point = GeometryUtils.createPoint(-22.50, -43.18);
        SafeLocation location = new SafeLocation(
                UUID.randomUUID(), "Ponto Seguro", "Centro", point,
                "Praça da Paz", SafeLocationType.COMMUNITY_CENTER, 80,
                SafeLocationOperationalStatus.OPEN, null
        );

        SafeLocationAvailability availability = location.computeAvailability(null);

        assertThat(availability).isEqualTo(SafeLocationAvailability.UNKNOWN);
        assertThat(availability).isNotEqualTo(SafeLocationAvailability.AVAILABLE);
    }

    @Test
    @DisplayName("RN042: Local com status operacional UNKNOWN deve ser classificado como UNKNOWN")
    void shouldComputeAvailabilityAsUnknownWhenOperationalStatusUnknown() {
        Point point = GeometryUtils.createPoint(-22.50, -43.18);
        SafeLocation location = new SafeLocation(
                UUID.randomUUID(), "Local Sem Verificação", "Descrição", point,
                "Endereço", SafeLocationType.OTHER, 50,
                SafeLocationOperationalStatus.UNKNOWN, null
        );

        SafeLocationAvailability availability = location.computeAvailability(10);

        assertThat(availability).isEqualTo(SafeLocationAvailability.UNKNOWN);
    }
}
