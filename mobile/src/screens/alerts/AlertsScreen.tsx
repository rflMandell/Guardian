import React, { useEffect, useState } from 'react';
import { useMobileAuth } from '../../contexts/AuthContext';
import { mobileApi } from '../../services/api';
import { Alert } from '../../types';

export const AlertsScreen: React.FC = () => {
  const { currentMunicipalityId } = useMobileAuth();
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!currentMunicipalityId) return;
    setLoading(true);
    mobileApi.getActiveAlerts(currentMunicipalityId)
      .then((data) => setAlerts(data))
      .catch((err) => console.error('Erro ao buscar alertas:', err))
      .finally(() => setLoading(false));
  }, [currentMunicipalityId]);

  const getSeverityStyle = (severity: string) => {
    switch (severity) {
      case 'CRITICAL':
        return { bg: '#fef2f2', border: '#ef4444', text: '#b91c1c' };
      case 'HIGH':
        return { bg: '#fff7ed', border: '#f97316', text: '#c2410c' };
      case 'MODERATE':
        return { bg: '#fffbeb', border: '#f59e0b', text: '#b45309' };
      default:
        return { bg: '#f0fdf4', border: '#22c55e', text: '#15803d' };
    }
  };

  return (
    <div style={{ padding: '20px', maxWidth: '500px', margin: '0 auto', paddingBottom: '90px' }}>
      <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: '#0f172a', marginBottom: '8px' }}>
        Alertas em Vigor
      </h2>
      <p style={{ color: '#64748b', fontSize: '0.85rem', marginBottom: '20px' }}>
        Comunicações oficiais de emergência e avisos emitidos para a sua região.
      </p>

      {loading ? (
        <p style={{ color: '#64748b', textAlign: 'center', padding: '40px 0' }}>Buscando alertas vigentes...</p>
      ) : alerts.length === 0 ? (
        <div style={{
          padding: '32px 20px',
          backgroundColor: '#f8fafc',
          borderRadius: '16px',
          textAlign: 'center',
          border: '1px solid #e2e8f0',
        }}>
          <span style={{ fontSize: '2.5rem' }}>✅</span>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#0f172a', marginTop: '12px' }}>
            Nenhum Alerta Ativo
          </h3>
          <p style={{ color: '#64748b', fontSize: '0.85rem', marginTop: '6px' }}>
            Não há alertas de risco severo ou evacuação vigentes para este município no momento.
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {alerts.map((alert) => {
            const style = getSeverityStyle(alert.severity);

            return (
              <div
                key={alert.id}
                style={{
                  backgroundColor: style.bg,
                  border: `2px solid ${style.border}`,
                  borderRadius: '16px',
                  padding: '20px',
                  boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.05)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <h3 style={{ fontSize: '1.1rem', fontWeight: 800, color: style.text, margin: 0 }}>
                    {alert.title}
                  </h3>
                  <span style={{
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    padding: '3px 8px',
                    borderRadius: '6px',
                    backgroundColor: style.border,
                    color: '#ffffff',
                  }}>
                    {alert.severity}
                  </span>
                </div>

                <p style={{ fontSize: '0.9rem', color: '#1e293b', marginTop: '10px', lineHeight: 1.5 }}>
                  {alert.description}
                </p>

                {alert.recommendations && alert.recommendations.length > 0 && (
                  <div style={{
                    marginTop: '16px',
                    padding: '12px',
                    backgroundColor: '#ffffff',
                    borderRadius: '10px',
                    border: '1px solid #e2e8f0',
                  }}>
                    <strong style={{ fontSize: '0.8rem', color: '#0f172a', display: 'block', marginBottom: '6px' }}>
                      Recomendações de Segurança:
                    </strong>
                    <ul style={{ margin: 0, paddingLeft: '18px', fontSize: '0.8rem', color: '#475569' }}>
                      {alert.recommendations.map((rec, i) => (
                        <li key={i} style={{ marginBottom: '4px' }}>{rec}</li>
                      ))}
                    </ul>
                  </div>
                )}

                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  marginTop: '14px',
                  fontSize: '0.75rem',
                  color: '#64748b',
                }}>
                  <span>Origem: <b>{alert.origin}</b></span>
                  <span>Expira em: <b>{new Date(alert.expiresAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</b></span>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
