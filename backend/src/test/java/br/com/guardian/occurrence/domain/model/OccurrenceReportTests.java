package br.com.guardian.occurrence.domain.model;

import br.com.guardian.geolocation.util.GeometryUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OccurrenceReport - Testes unitários do modelo de domínio de relatos")
class OccurrenceReportTests {

    @Test
    @DisplayName("Deve instanciar um relato anônimo com status inicial RECEIVED")
    void shouldCreateAnonymousOccurrenceReport() {
        UUID municipalityId = UUID.randomUUID();
        Point location = GeometryUtils.createPoint(-23.5505, -46.6333);
        Instant now = Instant.now();

        OccurrenceReport report = new OccurrenceReport(
                null,
                municipalityId,
                EventCategory.FLOODING,
                "Rua alagada próximo à ponte",
                location,
                now
        );

        assertThat(report.isAnonymous()).isTrue();
        assertThat(report.getUserId()).isNull();
        assertThat(report.getMunicipalityId()).isEqualTo(municipalityId);
        assertThat(report.getEventType()).isEqualTo(EventCategory.FLOODING);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.RECEIVED);
        assertThat(report.getDescription()).isEqualTo("Rua alagada próximo à ponte");
        assertThat(report.getLocation()).isEqualTo(location);
        assertThat(report.getReportedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Deve instanciar um relato identificado quando o userId for informado")
    void shouldCreateIdentifiedOccurrenceReport() {
        UUID userId = UUID.randomUUID();
        UUID municipalityId = UUID.randomUUID();
        Point location = GeometryUtils.createPoint(-23.5505, -46.6333);

        OccurrenceReport report = new OccurrenceReport(
                userId,
                municipalityId,
                EventCategory.LANDSLIDE,
                "Deslizamento de encosta",
                location,
                null
        );

        assertThat(report.isAnonymous()).isFalse();
        assertThat(report.getUserId()).isEqualTo(userId);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.RECEIVED);
    }

    @Test
    @DisplayName("Deve rejeitar criação de relato sem município obrigatório")
    void shouldRejectReportWithoutMunicipality() {
        Point location = GeometryUtils.createPoint(-23.5505, -46.6333);

        assertThatThrownBy(() -> new OccurrenceReport(
                null, null, EventCategory.FLASH_FLOOD, "Enxurrada", location, Instant.now()
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Município é obrigatório.");
    }

    @Test
    @DisplayName("Deve rejeitar criação de relato sem categoria de evento obrigatória")
    void shouldRejectReportWithoutEventCategory() {
        UUID municipalityId = UUID.randomUUID();
        Point location = GeometryUtils.createPoint(-23.5505, -46.6333);

        assertThatThrownBy(() -> new OccurrenceReport(
                null, municipalityId, null, "Descrição", location, Instant.now()
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Tipo de evento é obrigatório.");
    }

    @Test
    @DisplayName("Deve rejeitar criação de relato sem localização obrigatória")
    void shouldRejectReportWithoutLocation() {
        UUID municipalityId = UUID.randomUUID();

        assertThatThrownBy(() -> new OccurrenceReport(
                null, municipalityId, EventCategory.HEAVY_RAIN, "Chuva intensa", null, Instant.now()
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Localização é obrigatória.");
    }
}
