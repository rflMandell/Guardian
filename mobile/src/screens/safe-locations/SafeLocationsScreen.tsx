import React, { useEffect, useState } from 'react';
import { useMobileAuth } from '../../contexts/AuthContext';
import { mobileApi } from '../../services/api';
import { getCurrentLocation, calculateDistanceMeters, Coordinates } from '../../utils/geolocation';
import { SafeLocation } from '../../types';

export const SafeLocationsScreen: React.FC = () => {
  const { currentMunicipalityId } = useMobileAuth();
  const [locations, setLocations] = useState<SafeLocation[]>([]);
  const [coords, setCoords] = useState<Coordinates | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getCurrentLocation().then((loc) => setCoords(loc));
  }, []);

  useEffect(() => {
    if (!currentMunicipalityId) return;
    setLoading(true);
    mobileApi.getSafeLocations(currentMunicipalityId, false)
      .then((data) => setLocations(data))
      .catch((err) => console.error('Erro ao buscar abrigos:', err))
      .finally(() => setLoading(false));
  }, [currentMunicipalityId]);

  const getAvailabilityStyle = (availability: string) => {
    switch (availability) {
      case 'AVAILABLE':
        return { bg: '#ecfdf5', text: '#047857', label: 'Disponível' };
      case 'LIMITED':
        return { bg: '#fffbeb', text: '#b45309', label: 'Vagas Limitadas' };
      case 'FULL':
        return { bg: '#fef2f2', text: '#b91c1c', label: 'Lotado' };
      default:
        return { bg: '#f1f5f9', text: '#475569', label: 'Indisponível' };
    }
  };

  // Ordenar por distância quando as coordenadas estiverem disponíveis
  const sortedLocations = [...locations].sort((a, b) => {
    if (!coords) return 0;
    const distA = calculateDistanceMeters(coords, { latitude: a.latitude, longitude: a.longitude });
    const distB = calculateDistanceMeters(coords, { latitude: b.latitude, longitude: b.longitude });
    return distA - distB;
  });

  return (
    <div style={{ padding: '20px', maxWidth: '500px', margin: '0 auto', paddingBottom: '90px' }}>
      <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: '#0f172a', marginBottom: '8px' }}>
        Pontos Seguros e Acolhimento
      </h2>
      <p style={{ color: '#64748b', fontSize: '0.85rem', marginBottom: '20px' }}>
        Locais monitorados pela Defesa Civil para abrigo temporário e assistência humanitária.
      </p>

      {loading ? (
        <p style={{ color: '#64748b', textAlign: 'center', padding: '40px 0' }}>Localizando abrigos seguros...</p>
      ) : sortedLocations.length === 0 ? (
        <div style={{ padding: '32px 20px', backgroundColor: '#f8fafc', borderRadius: '16px', textAlign: 'center' }}>
          <p style={{ color: '#64748b' }}>Nenhum ponto seguro registrado neste município.</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          {sortedLocations.map((loc) => {
            const avail = getAvailabilityStyle(loc.availability);
            const distMeters = coords
              ? calculateDistanceMeters(coords, { latitude: loc.latitude, longitude: loc.longitude })
              : null;

            return (
              <div
                key={loc.id}
                style={{
                  backgroundColor: '#ffffff',
                  borderRadius: '14px',
                  padding: '18px',
                  border: '1px solid #e2e8f0',
                  boxShadow: '0 2px 4px rgb(0 0 0 / 0.04)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#0f172a', margin: 0 }}>
                    {loc.name}
                  </h3>
                  <span style={{
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    padding: '3px 8px',
                    borderRadius: '6px',
                    backgroundColor: avail.bg,
                    color: avail.text,
                  }}>
                    {avail.label}
                  </span>
                </div>

                {loc.address && (
                  <p style={{ fontSize: '0.825rem', color: '#475569', marginTop: '6px', margin: 0 }}>
                    📍 {loc.address}
                  </p>
                )}

                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginTop: '12px',
                  paddingTop: '10px',
                  borderTop: '1px solid #f1f5f9',
                  fontSize: '0.8rem',
                  color: '#64748b',
                }}>
                  <span>
                    {distMeters !== null
                      ? distMeters >= 1000
                        ? `A ${(distMeters / 1000).toFixed(1)} km de você`
                        : `A ${distMeters} m de você`
                      : 'Distância calculando...'}
                  </span>
                  <span>
                    Ocupação: <b>{loc.currentOccupancy ?? 0}</b> / {loc.capacity ?? '?'}
                  </span>
                </div>

                {loc.contactPhone && (
                  <div style={{ fontSize: '0.75rem', color: '#2563eb', marginTop: '8px' }}>
                    📞 Contato: {loc.contactPhone}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
