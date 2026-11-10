package br.com.guardian.geolocation.util;

import br.com.guardian.shared.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Coordinates - Testes unitários de validação de coordenadas")
class CoordinatesTests {

    @Test
    @DisplayName("Deve instanciar coordenadas válidas com sucesso")
    void shouldCreateValidCoordinates() {
        Coordinates coords = new Coordinates(-23.5505, -46.6333);

        assertThat(coords.latitude()).isEqualTo(-23.5505);
        assertThat(coords.longitude()).isEqualTo(-46.6333);
    }

    @ParameterizedTest
    @CsvSource({
            "-90.0, 0.0",
            "90.0, 0.0",
            "0.0, -180.0",
            "0.0, 180.0",
            "-90.0, -180.0",
            "90.0, 180.0"
    })
    @DisplayName("Deve aceitar limites exatos de latitude [-90, 90] e longitude [-180, 180]")
    void shouldAcceptBoundaryCoordinates(Double lat, Double lon) {
        Coordinates coords = new Coordinates(lat, lon);
        assertThat(coords.latitude()).isEqualTo(lat);
        assertThat(coords.longitude()).isEqualTo(lon);
    }

    @ParameterizedTest
    @CsvSource({
            "-90.0001, 0.0",
            "90.0001, 0.0",
            "-120.0, 45.0",
            "95.0, -40.0"
    })
    @DisplayName("Deve lançar ValidationException quando a latitude estiver fora do intervalo [-90, 90]")
    void shouldRejectInvalidLatitude(Double lat, Double lon) {
        assertThatThrownBy(() -> new Coordinates(lat, lon))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Coordenadas geográficas inválidas.");
    }

    @ParameterizedTest
    @CsvSource({
            "0.0, -180.0001",
            "0.0, 180.0001",
            "-20.0, -185.0",
            "20.0, 190.0"
    })
    @DisplayName("Deve lançar ValidationException quando a longitude estiver fora do intervalo [-180, 180]")
    void shouldRejectInvalidLongitude(Double lat, Double lon) {
        assertThatThrownBy(() -> new Coordinates(lat, lon))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Coordenadas geográficas inválidas.");
    }

    @Test
    @DisplayName("Deve rejeitar latitude nula")
    void shouldRejectNullLatitude() {
        assertThatThrownBy(() -> new Coordinates(null, -46.6333))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar longitude nula")
    void shouldRejectNullLongitude() {
        assertThatThrownBy(() -> new Coordinates(-23.5505, null))
                .isInstanceOf(ValidationException.class);
    }
}
