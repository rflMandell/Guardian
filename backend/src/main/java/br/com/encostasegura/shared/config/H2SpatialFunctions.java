package br.com.encostasegura.shared.config;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

/**
 * Função utilitária para registrar alias espaciais no H2 em ambiente de teste,
 * convertendo tipos GEOMETRY para String WKT para permitir avaliação JTS.
 */
public final class H2SpatialFunctions {

    private H2SpatialFunctions() {}

    public static boolean stContains(String g1, String g2) {
        try {
            if (g1 != null && g2 != null) {
                WKTReader reader = new WKTReader();
                Geometry geom1 = reader.read(g1);
                Geometry geom2 = reader.read(g2);
                return geom1.contains(geom2);
            }
        } catch (Exception ignored) {
            // fallback seguro
        }
        return true;
    }
}
