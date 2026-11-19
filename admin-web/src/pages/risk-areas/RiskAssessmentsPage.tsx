import React, { useEffect, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { api } from '../../services/api';
import { RiskAssessment, RiskLevel } from '../../types';

export const RiskAssessmentsPage: React.FC = () => {
  const { selectedMunicipalityId } = useAuth();
  const [assessment, setAssessment] = useState<RiskAssessment | null>(null);
  const [loading, setLoading] = useState(false);
  const [recalculating, setRecalculating] = useState(false);
  const [history, setHistory] = useState<RiskAssessment[]>([]);

  const loadRiskData = () => {
    if (!selectedMunicipalityId) return;
    setLoading(true);
    Promise.allSettled([
      api.getLatestRisk(selectedMunicipalityId),
      api.listRiskAssessments(selectedMunicipalityId, 0, 10),
    ])
      .then(([latestRes, histRes]) => {
        if (latestRes.status === 'fulfilled') setAssessment(latestRes.value);
        if (histRes.status === 'fulfilled') setHistory(histRes.value.data);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadRiskData();
  }, [selectedMunicipalityId]);

  const handleRecalculate = async () => {
    if (!selectedMunicipalityId) return;
    setRecalculating(true);
    try {
      const updated = await api.recalculateRisk(selectedMunicipalityId);
      setAssessment(updated);
      loadRiskData();
    } catch (err: unknown) {
      if (err instanceof Error) {
        alert(err.message);
      } else {
        alert('Erro ao recalcular avaliação de risco.');
      }
    } finally {
      setRecalculating(false);
    }
  };

  const getRiskBadgeClass = (level: RiskLevel) => {
    switch (level) {
      case 'CRITICAL': return 'badge-critical';
      case 'HIGH': return 'badge-high';
      case 'MODERATE': return 'badge-moderate';
      case 'LOW': return 'badge-low';
    }
  };

  return (
    <div>
      {/* Top Banner / Assessment Card */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <h3 className="card-title" style={{ margin: 0 }}>Avaliação de Risco Atual</h3>
              {assessment && (
                <span className={`badge ${getRiskBadgeClass(assessment.overallRiskLevel)}`} style={{ fontSize: '0.9rem', padding: '4px 10px' }}>
                  {assessment.overallRiskLevel}
                </span>
              )}
            </div>
            <p style={{ color: '#64748b', fontSize: '0.875rem', marginTop: '6px' }}>
              {assessment?.summary || 'Nenhuma avaliação processada para este município.'}
            </p>
          </div>

          <button
            className="btn btn-primary"
            onClick={handleRecalculate}
            disabled={recalculating || loading}
          >
            {recalculating ? 'Processando Motor de Regras...' : '🔄 Recalcular Risco Agora'}
          </button>
        </div>

        {assessment && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '16px', marginTop: '24px', paddingTop: '20px', borderTop: '1px solid #e2e8f0' }}>
            <div>
              <span className="stat-label">Confiabilidade</span>
              <div style={{ fontWeight: 600, marginTop: '4px' }}>{assessment.confidenceLevel}</div>
            </div>
            <div>
              <span className="stat-label">Status dos Dados</span>
              <div style={{ fontWeight: 600, marginTop: '4px' }}>
                <span className={`badge ${assessment.status === 'VALID' ? 'badge-low' : 'badge-moderate'}`}>
                  {assessment.status}
                </span>
              </div>
            </div>
            <div>
              <span className="stat-label">Data da Avaliação</span>
              <div style={{ fontSize: '0.85rem', marginTop: '4px', color: '#334155' }}>
                {new Date(assessment.evaluatedAt).toLocaleString()}
              </div>
            </div>
            <div>
              <span className="stat-label">Válido Até</span>
              <div style={{ fontSize: '0.85rem', marginTop: '4px', color: '#334155' }}>
                {new Date(assessment.validUntil).toLocaleString()}
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Explanatory Factors (Explainability RN020) */}
      <div className="card">
        <h3 className="card-title">Fatores Determinísticos da Avaliação (Explicabilidade)</h3>
        <p style={{ color: '#64748b', fontSize: '0.875rem', marginBottom: '16px' }}>
          O Encosta Segura não oculta ausência de dados como ausência de risco e detalha a contribuição de cada fator observado.
        </p>

        {assessment?.factors && assessment.factors.length > 0 ? (
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>Fator Observado</th>
                  <th>Peso</th>
                  <th>Contribuição</th>
                  <th>Descrição / Justificativa</th>
                </tr>
              </thead>
              <tbody>
                {assessment.factors.map((f, idx) => (
                  <tr key={idx}>
                    <td><strong>{f.factorName}</strong></td>
                    <td>{f.weight}</td>
                    <td>
                      <span className="badge badge-active">{f.contributionLevel}</span>
                    </td>
                    <td>{f.description}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p style={{ color: '#64748b', fontSize: '0.875rem' }}>
            Nenhum fator discriminado registrado na última avaliação.
          </p>
        )}
      </div>

      {/* History */}
      <div className="card">
        <h3 className="card-title">Histórico de Avaliações</h3>
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Data / Hora</th>
                <th>Nível Geral</th>
                <th>Status</th>
                <th>Confiabilidade</th>
                <th>Resumo</th>
              </tr>
            </thead>
            <tbody>
              {history.map((h) => (
                <tr key={h.id}>
                  <td>{new Date(h.evaluatedAt).toLocaleString()}</td>
                  <td>
                    <span className={`badge ${getRiskBadgeClass(h.overallRiskLevel)}`}>
                      {h.overallRiskLevel}
                    </span>
                  </td>
                  <td>{h.status}</td>
                  <td>{h.confidenceLevel}</td>
                  <td style={{ fontSize: '0.85rem' }}>{h.summary}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
