import React, { useEffect, useState } from 'react';
import { useMobileAuth } from '../../contexts/AuthContext';
import { mobileApi } from '../../services/api';
import { getCurrentLocation, Coordinates } from '../../utils/geolocation';
import { Alert, RiskAssessment, Municipality } from '../../types';

interface HomeScreenProps {
  onNavigate: (tab: string) => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({ onNavigate }) => {
  const { user, currentMunicipalityId, setCurrentMunicipalityId, logout } = useMobileAuth();
  const [coords, setCoords] = useState<Coordinates | null>(null);
  const [municipalities, setMunicipalities] = useState<Municipality[]>([]);
  const [activeAlerts, setActiveAlerts] = useState<Alert[]>([]);
  const [riskAssessment, setRiskAssessment] = useState<RiskAssessment | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // 1. Obter geolocalização do dispositivo
    getCurrentLocation().then((loc) => setCoords(loc));

    // 2. Listar municípios monitorados
    mobileApi.getMunicipalities().then((list) => {
      setMunicipalities(list);
      if (!currentMunicipalityId && list.length > 0) {
        setCurrentMunicipalityId(list[0].id);
      }
    });
  }, [currentMunicipalityId, setCurrentMunicipalityId]);

  useEffect(() => {
    if (!currentMunicipalityId) return;

    setLoading(true);
    Promise.allSettled([
      mobileApi.getActiveAlerts(currentMunicipalityId),
      mobileApi.getLatestRisk(currentMunicipalityId),
    ]).then(([alertsRes, riskRes]) => {
      if (alertsRes.status === 'fulfilled') setActiveAlerts(alertsRes.value);
      if (riskRes.status === 'fulfilled') setRiskAssessment(riskRes.value);
      setLoading(false);
    });
  }, [currentMunicipalityId]);

  const getRiskColor = (level?: string) => {
    switch (level) {
      case 'CRITICAL': return '#ef4444';
      case 'HIGH': return '#f97316';
      case 'MODERATE': return '#f59e0b';
      case 'LOW': return '#10b981';
      default: return '#64748b';
    }
  };

  const selectedMun = municipalities.find((m) => m.id === currentMunicipalityId);

  return (
    <div style={{ padding: '20px', maxWidth: '500px', margin: '0 auto', paddingBottom: '90px' }}>
      {/* Top Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <div>
          <span style={{ fontSize: '0.8rem', color: '#64748b' }}>Você está em</span>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#0f172a', margin: 0 }}>
              {selectedMun ? `${selectedMun.name} - ${selectedMun.state}` : 'Localizando...'}
            </h2>
          </div>
        </div>

        {user ? (
          <button
            onClick={logout}
            style={{
              padding: '6px 12px',
              fontSize: '0.75rem',
              borderRadius: '6px',
              border: '1px solid #cbd5e1',
              backgroundColor: '#ffffff',
              color: '#ef4444',
              cursor: 'pointer',
            }}
          >
            Sair ({user.name.split(' ')[0]})
          </button>
        ) : (
          <button
            onClick={() => onNavigate('login')}
            style={{
              padding: '6px 12px',
              fontSize: '0.75rem',
              borderRadius: '6px',
              backgroundColor: '#2563eb',
              color: '#ffffff',
              border: 'none',
              cursor: 'pointer',
            }}
          >
            Entrar
          </button>
        )}
      </div>

      {/* GPS info bar */}
      {coords && (
        <div style={{
          fontSize: '0.75rem',
          color: '#64748b',
          backgroundColor: '#f1f5f9',
          padding: '6px 12px',
          borderRadius: '6px',
          marginBottom: '20px',
          display: 'flex',
          justifyContent: 'space-between',
        }}>
          <span>📍 GPS: {coords.latitude.toFixed(4)}, {coords.longitude.toFixed(4)}</span>
          <span style={{ color: '#10b981', fontWeight: 600 }}>● Cobertura Ativa</span>
        </div>
      )}

      {/* Active Alerts Banner */}
      {activeAlerts.length > 0 && (
        <div
          onClick={() => onNavigate('alerts')}
          style={{
            backgroundColor: '#fef2f2',
            border: '2px solid #ef4444',
            borderRadius: '12px',
            padding: '16px',
            marginBottom: '20px',
            cursor: 'pointer',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '1.2rem' }}>🚨</span>
            <strong style={{ color: '#b91c1c', fontSize: '1rem' }}>
              {activeAlerts.length} ALERTA{activeAlerts.length > 1 ? 'S' : ''} VIGENTE{activeAlerts.length > 1 ? 'S' : ''}!
            </strong>
          </div>
          <p style={{ fontSize: '0.875rem', color: '#7f1d1d', marginTop: '6px', margin: 0 }}>
            {activeAlerts[0].title}
          </p>
          <span style={{ fontSize: '0.75rem', color: '#991b1b', textDecoration: 'underline', marginTop: '8px', display: 'block' }}>
            Toque para ver orientações e detalhes de segurança →
          </span>
        </div>
      )}

      {/* Main Risk Card */}
      <div
        onClick={() => onNavigate('risk')}
        style={{
          backgroundColor: '#ffffff',
          borderRadius: '16px',
          padding: '24px',
          boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.08)',
          border: '1px solid #e2e8f0',
          marginBottom: '24px',
          cursor: 'pointer',
        }}
      >
        <span style={{ fontSize: '0.8rem', color: '#64748b', textTransform: 'uppercase', fontWeight: 600 }}>
          Monitoramento Encosta Segura
        </span>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '8px' }}>
          <div>
            <h3 style={{ fontSize: '1.6rem', fontWeight: 800, color: getRiskColor(riskAssessment?.overallRiskLevel), margin: 0 }}>
              {loading ? 'Calculando...' : `RISCO ${riskAssessment?.overallRiskLevel || 'NORMAL'}`}
            </h3>
            <span style={{ fontSize: '0.8rem', color: '#64748b' }}>
              Confiabilidade: {riskAssessment?.confidenceLevel || 'MÉDIA'} • Status: {riskAssessment?.status || 'VÁLIDO'}
            </span>
          </div>
          <div style={{
            width: '48px',
            height: '48px',
            borderRadius: '24px',
            backgroundColor: `${getRiskColor(riskAssessment?.overallRiskLevel)}22`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '1.5rem',
          }}>
            ⚠️
          </div>
        </div>
        <p style={{ fontSize: '0.875rem', color: '#334155', marginTop: '12px' }}>
          {riskAssessment?.summary || 'Nenhum risco severo iminente detectado nas últimas horas.'}
        </p>
      </div>

      {/* Quick Action Grid */}
      <h4 style={{ fontSize: '1rem', fontWeight: 700, color: '#0f172a', marginBottom: '12px' }}>
        Ações Rápidas
      </h4>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
        <button
          onClick={() => onNavigate('occurrences')}
          style={{
            padding: '18px 14px',
            borderRadius: '12px',
            backgroundColor: '#ffffff',
            border: '1px solid #cbd5e1',
            boxShadow: '0 2px 4px rgb(0 0 0 / 0.05)',
            textAlign: 'left',
            cursor: 'pointer',
            display: 'flex',
            flexDirection: 'column',
            gap: '8px',
          }}
        >
          <span style={{ fontSize: '1.6rem' }}>📝</span>
          <strong style={{ fontSize: '0.95rem', color: '#0f172a' }}>Registrar Relato</strong>
          <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Informar alagamento ou deslizamento</span>
        </button>

        <button
          onClick={() => onNavigate('safe-locations')}
          style={{
            padding: '18px 14px',
            borderRadius: '12px',
            backgroundColor: '#ffffff',
            border: '1px solid #cbd5e1',
            boxShadow: '0 2px 4px rgb(0 0 0 / 0.05)',
            textAlign: 'left',
            cursor: 'pointer',
            display: 'flex',
            flexDirection: 'column',
            gap: '8px',
          }}
        >
          <span style={{ fontSize: '1.6rem' }}>🛡️</span>
          <strong style={{ fontSize: '0.95rem', color: '#0f172a' }}>Abrigos Seguros</strong>
          <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Pontos de acolhimento próximos</span>
        </button>
      </div>
    </div>
  );
};
