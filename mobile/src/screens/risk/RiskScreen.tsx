import React, { useEffect, useState } from 'react';
import { useMobileAuth } from '../../contexts/AuthContext';
import { mobileApi } from '../../services/api';
import { RiskAssessment } from '../../types';

export const RiskScreen: React.FC = () => {
  const { currentMunicipalityId } = useMobileAuth();
  const [assessment, setAssessment] = useState<RiskAssessment | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!currentMunicipalityId) return;
    setLoading(true);
    mobileApi.getLatestRisk(currentMunicipalityId)
      .then((data) => setAssessment(data))
      .catch((err) => console.error('Erro ao carregar risco:', err))
      .finally(() => setLoading(false));
  }, [currentMunicipalityId]);

  return (
    <div style={{ padding: '20px', maxWidth: '500px', margin: '0 auto', paddingBottom: '90px' }}>
      <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: '#0f172a', marginBottom: '8px' }}>
        Avaliação de Risco Urbano
      </h2>
      <p style={{ color: '#64748b', fontSize: '0.85rem', marginBottom: '20px' }}>
        Transparência e explicabilidade determinística das análises municipais.
      </p>

      {loading ? (
        <p style={{ color: '#64748b', textAlign: 'center', padding: '40px 0' }}>Consultando modelos de risco...</p>
      ) : !assessment ? (
        <div style={{ padding: '24px', backgroundColor: '#f1f5f9', borderRadius: '12px', textAlign: 'center' }}>
          <p style={{ color: '#475569' }}>Nenhum dado de risco disponível para esta região.</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div style={{
            backgroundColor: '#ffffff',
            borderRadius: '14px',
            padding: '20px',
            border: '1px solid #e2e8f0',
            boxShadow: '0 2px 4px rgb(0 0 0 / 0.05)',
          }}>
            <span style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 600 }}>NÍVEL IDENTIFICADO</span>
            <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#2563eb', marginTop: '4px' }}>
              {assessment.overallRiskLevel}
            </div>
            <p style={{ fontSize: '0.9rem', color: '#334155', marginTop: '8px' }}>
              {assessment.summary}
            </p>

            <div style={{
              display: 'flex',
              justifyContent: 'space-between',
              marginTop: '16px',
              paddingTop: '12px',
              borderTop: '1px solid #f1f5f9',
              fontSize: '0.75rem',
              color: '#64748b',
            }}>
              <span>Confiabilidade: <b>{assessment.confidenceLevel}</b></span>
              <span>Status: <b>{assessment.status}</b></span>
            </div>
          </div>

          {/* Fatores Explicativos */}
          <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#0f172a', marginTop: '8px' }}>
            Fatores que Justificam esta Análise
          </h3>

          {assessment.factors?.map((factor, idx) => (
            <div
              key={idx}
              style={{
                backgroundColor: '#ffffff',
                borderRadius: '12px',
                padding: '16px',
                border: '1px solid #e2e8f0',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <strong style={{ fontSize: '0.9rem', color: '#0f172a' }}>{factor.factorName}</strong>
                <span style={{
                  fontSize: '0.75rem',
                  padding: '2px 8px',
                  borderRadius: '6px',
                  backgroundColor: '#eff6ff',
                  color: '#2563eb',
                  fontWeight: 600,
                }}>
                  {factor.contributionLevel}
                </span>
              </div>
              <p style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '6px' }}>
                {factor.description}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
