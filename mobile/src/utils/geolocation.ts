export interface Coordinates {
  latitude: number;
  longitude: number;
}

// Município de Demonstração (São Paulo/Área Urbana)
export const DEFAULT_DEMO_COORDINATES: Coordinates = {
  latitude: -23.55052,
  longitude: -46.633308,
};

export const getCurrentLocation = async (): Promise<Coordinates> => {
  if (typeof navigator !== 'undefined' && 'geolocation' in navigator) {
    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          resolve({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          });
        },
        () => {
          // Fallback para coordenadas de demonstração em caso de recusa/erro de sensor
          resolve(DEFAULT_DEMO_COORDINATES);
        },
        { timeout: 5000, enableHighAccuracy: true }
      );
    });
  }
  return DEFAULT_DEMO_COORDINATES;
};

export const calculateDistanceMeters = (
  c1: Coordinates,
  c2: Coordinates
): number => {
  const R = 6371e3; // Raio da Terra em metros
  const phi1 = (c1.latitude * Math.PI) / 180;
  const phi2 = (c2.latitude * Math.PI) / 180;
  const deltaPhi = ((c2.latitude - c1.latitude) * Math.PI) / 180;
  const deltaLambda = ((c2.longitude - c1.longitude) * Math.PI) / 180;

  const a =
    Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
    Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return Math.round(R * c);
};
