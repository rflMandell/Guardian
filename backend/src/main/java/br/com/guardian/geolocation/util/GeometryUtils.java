package br.com.guardian.geolocation.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

public final class GeometryUtils {

    public static final int SRID = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), SRID);

    private GeometryUtils() {}

    public static GeometryFactory getFactory() {
        return GEOMETRY_FACTORY;
    }

    /**
     * Cria um Ponto JTS no padrão SRID 4326.
     * Importante: Em sistemas GIS/JTS, X é longitude e Y é latitude.
     */
    public static Point createPoint(Double latitude, Double longitude) {
        Coordinates coords = new Coordinates(latitude, longitude);
        return GEOMETRY_FACTORY.createPoint(new Coordinate(coords.longitude(), coords.latitude()));
    }

    public static MultiPolygon createMultiPolygon(Polygon... polygons) {
        return GEOMETRY_FACTORY.createMultiPolygon(polygons);
    }
}
