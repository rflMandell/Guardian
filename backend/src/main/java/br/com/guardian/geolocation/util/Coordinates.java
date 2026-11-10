package br.com.guardian.geolocation.util;

import br.com.guardian.shared.exception.FieldErrorDetail;
import br.com.guardian.shared.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;

public record Coordinates(Double latitude, Double longitude) {

    public Coordinates {
        List<FieldErrorDetail> errors = new ArrayList<>();

        if (latitude == null) {
            errors.add(new FieldErrorDetail("latitude", "A latitude é obrigatória."));
        } else if (latitude < -90.0 || latitude > 90.0) {
            errors.add(new FieldErrorDetail("latitude", "A latitude deve estar entre -90 e 90."));
        }

        if (longitude == null) {
            errors.add(new FieldErrorDetail("longitude", "A longitude é obrigatória."));
        } else if (longitude < -180.0 || longitude > 180.0) {
            errors.add(new FieldErrorDetail("longitude", "A longitude deve estar entre -180 e 180."));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Coordenadas geográficas inválidas.", errors);
        }
    }

    public static Coordinates of(Double latitude, Double longitude) {
        return new Coordinates(latitude, longitude);
    }
}
