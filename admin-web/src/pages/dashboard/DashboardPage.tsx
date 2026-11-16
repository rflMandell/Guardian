import React, { useEffect, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { api } from '../../services/api';
import { Alert, OccurrenceReport, RiskAssessment } from '../../types';

interface DashboardPageProps {
  onNavigate: (tab: string) => void;
}

export const DashboardPage: React.FC<DashboardPageProps> = ({ onNavigate }) => {
  const { selectedMunicipalityId } = useAuth();
  const [loading, setLoading] = useState(true);
  const [activeAlerts, setActiveAlerts] = useState<Alert[]>([]);
  const [latestRisk, setLatestRisk] = useState<RiskAssessment | null>(null);
  const [occurrences, setOccurrences] = useState<OccurrenceReport[]>([]);
  const [safeLocationsCount, setSafeLocationsCount] = useState(0);

  useEffect(() => {
    if (!selectedMunicipalityId) return;

    setLoading(true);
    Promise.allSettled([
      api.listAlerts({ municipalityId: selectedMunicipalityId, activeOnly: true }),
      api.getLatestRisk(selectedMunicipalityId),
      api.listOccurrences({ municipalityId: selectedMunicipalityId, size: 5 }),
      api.listSafeLocations({ municipalityId: selectedMunicipalityId, availableOnly: true }),
    ]).then(([alertsRes, riskRes, occRes, safeRes]) => {
      if (alertsRes.status === 'fulfilled') setActiveAlerts(alertsRes.value.data);
      if (riskRes.status === 'fulfilled') setLatestRisk(riskRes.value);
      if (occRes.status === 'fulfilled') setOccurrences(occRes.value.data);
      if (safeRes.status === 'fulfilled') setSafeLocationsCount(safeRes.value.pagination.totalElements);
      setLoading(false);
    });
  }, [selectedMunicipalityId]);

  const getRiskBadgeClass = (level?: string) => {
    switch (level) {
      case 'CRITICAL': return 'badge-critical';
      case 'HIGH': return 'badge-high';
      case 'MODERATE': return 'badge-moderate';
      case 'LOW': return 'badge-low';
      default: return 'badge-closed';
    }
  };

  if (!selectedMunicipalityId) {
    return (
      <div className="card" style={{ textAlign: 'center', padding: '48px' }}>
        <p style={{ color: '#64748b' }}>Nenhum município selecionado para monitoramento.</p>
      </div>
    );
  }

  return (
    <div>
      {/* Metric Cards */}
      <div className="stats-grid">
        <div className="stat-card" style={{ borderLeft: '4px solid #ef4444' }}>
          <span className="stat-label">Alertas Vigentes</span>
          <span className="stat-value" style={{ color: activeAlerts.length > 0 ? '#ef4444' : '#0f172a' }}>
            {loading ? '...' : activeAlerts.length}
          </span>
          <span style={{ fontSize: '0.8rem', color: '#64748b' }}>
            {activeAlerts.length > 0 ? 'Exige atenção da Defesa Civil' : 'Nenhum alerta crítico ativo'}
          </span>
        </div>

        <div className="stat-card" style={{ borderLeft: '4px solid #f59e0b' }}>
          <span className="stat-label">Nível de Risco Municipal</span>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '4px' }}>
            <span className={`badge ${getRiskBadgeClass(latestRisk?.overallRiskLevel)}`} style={{ fontSize: '1rem', padding: '6px 12px' }}>
              {loading ? '...' : (latestRisk?.overallRiskLevel || 'INDEFINIDO')}
            </span>
          </div>
          <span style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '4px' }}>
            Status: {latestRisk?.status || 'N/A'} (Confiança: {latestRisk?.confidenceLevel || 'N/A'})
          </span>
        </div>

        <div className="stat-card" style={{ borderLeft: '4px solid #2563eb' }}>
          <span className="stat-label">Relatos Recentes de Cidadãos</span>
          <span className="stat-value">{loading ? '...' : occurrences.length}</span>
          <span style={{ fontSize: '0.8rem', color: '#64748b' }}>
            {occurrences.filter((o) => o.status === 'RECEIVED').length} aguardando análise
          </span>
        </div>

        <div className="stat-card" style={{ borderLeft: '4px solid #10b981' }}>
          <span className="stat-label">Pontos Seguros Disponíveis</span>
          <span className="stat-value" style={{ color: '#10b981' }}>
            {loading ? '...' : safeLocationsCount}
          </span>
          <span style={{ fontSize: '0.8rem', color: '#64748b' }}>Acolhimento imediato verificado</span>
        </div>
      </div>

      {/* Grid of details */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>
        {/* Alertas Ativos */}
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 className="card-title" style={{ margin: 0 }}>🚨 Alertas em Vigor</h3>
            <button className="btn btn-primary btn-sm" onClick={() => onNavigate('alerts')}>
              Gerenciar Alertas
            </button>
          </div>

          {activeAlerts.length === 0 ? (
            <p style={{ color: '#64748b', fontSize: '0.9rem', padding: '24px 0', textAlign: 'center' }}>
              Nenhum alerta ativo neste município no momento.
            </p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {activeAlerts.map((alert) => (
                <div
                  key={alert.id}
                  style={{
                    padding: '14px',
                    borderRadius: '8px',
                    backgroundColor: '#fff7ed',
                    border: '1px solid #fed7aa',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <h4 style={{ fontSize: '0.95rem', fontWeight: 600, color: '#9a3412' }}>{alert.title}</h4>
                    <span className={`badge ${getRiskBadgeClass(alert.severity)}`}>{alert.severity}</span>
                  </div>
                  <p style={{ fontSize: '0.85rem', color: '#431407', marginTop: '6px' }}>{alert.description}</p>
                  <div style={{ fontSize: '0.75rem', color: '#7c2d12', marginTop: '8px' }}>
                    Categoria: <b>{alert.category}</b> • Expira em: {new Date(alert.expiresAt).toLocaleTimeString()}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Ocorrências Recentes */}
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 className="card-title" style={{ margin: 0 }}>📝 Últimos Relatos de Ocorrência</h3>
            <button className="btn btn-secondary btn-sm" onClick={() => onNavigate('occurrences')}>
              Ver Todas
            </button>
          </div>

          {occurrences.length === 0 ? (
            <p style={{ color: '#64748b', fontSize: '0.9rem', padding: '24px 0', textAlign: 'center' }}>
              Nenhum relato recente cadastrado.
            </p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {occurrences.slice(0, 4).map((occ) => (
                <div
                  key={occ.id}
                  style={{
                    padding: '12px',
                    borderRadius: '8px',
                    border: '1px solid #e2e8f0',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                  }}
                >
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span style={{ fontWeight: 600, fontSize: '0.875rem' }}>{occ.category}</span>
                      <span className={`badge badge-${occ.status.toLowerCase()}`}>{occ.status}</span>
                    </div>
                    <p style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '4px' }}>
                      {occ.description.length > 50 ? `${occ.description.slice(0, 50)}...` : occ.description}
                    </p>
                  </div>
                  <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                    {new Date(occ.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
