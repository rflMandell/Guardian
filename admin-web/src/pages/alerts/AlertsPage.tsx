import React, { useEffect, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { api } from '../../services/api';
import { Alert, AlertSeverity, EventCategory } from '../../types';

export const AlertsPage: React.FC = () => {
  const { selectedMunicipalityId } = useAuth();
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [loading, setLoading] = useState(false);
  const [activeOnly, setActiveOnly] = useState(false);

  // Modal Emitir Alerta
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState<EventCategory>('FLOODING');
  const [severity, setSeverity] = useState<AlertSeverity>('HIGH');
  const [expiresAtHours, setExpiresAtHours] = useState('4');
  const [recommendationsText, setRecommendationsText] = useState('Evite transitar por vias alagadas\nProcure abrigo em local elevado');
  const [creating, setCreating] = useState(false);

  // Modal Encerrar Alerta
  const [closingAlert, setClosingAlert] = useState<Alert | null>(null);
  const [closeReason, setCloseReason] = useState('');
  const [closing, setClosing] = useState(false);

  const loadAlerts = () => {
    if (!selectedMunicipalityId) return;
    setLoading(true);
    api.listAlerts({
      municipalityId: selectedMunicipalityId,
      activeOnly: activeOnly || undefined,
      size: 50,
    })
      .then((res) => setAlerts(res.data))
      .catch((err) => console.error('Erro ao buscar alertas:', err))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadAlerts();
  }, [selectedMunicipalityId, activeOnly]);

  const handleCreateAlert = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedMunicipalityId) return;

    setCreating(true);
    try {
      const expirationDate = new Date();
      expirationDate.setHours(expirationDate.getHours() + parseInt(expiresAtHours, 10));

      const recs = recommendationsText
        .split('\n')
        .map((r) => r.trim())
        .filter((r) => r.length > 0);

      await api.createAlert({
        municipalityId: selectedMunicipalityId,
        title,
        description,
        category,
        severity,
        expiresAt: expirationDate.toISOString(),
        recommendations: recs,
      });

      setShowCreateModal(false);
      setTitle('');
      setDescription('');
      loadAlerts();
    } catch (err: unknown) {
      if (err instanceof Error) {
        alert(err.message);
      } else {
        alert('Erro ao emitir alerta.');
      }
    } finally {
      setCreating(false);
    }
  };

  const handleCloseAlert = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!closingAlert) return;
    if (!closeReason.trim()) {
      alert('A justificativa de encerramento é obrigatória (Regra RN033).');
      return;
    }

    setClosing(true);
    try {
      await api.closeAlert(closingAlert.id, closeReason);
      setClosingAlert(null);
      setCloseReason('');
      loadAlerts();
    } catch (err: unknown) {
      if (err instanceof Error) {
        alert(err.message);
      } else {
        alert('Erro ao encerrar alerta.');
      }
    } finally {
      setClosing(false);
    }
  };

  const getSeverityBadgeClass = (sev: AlertSeverity) => {
    switch (sev) {
      case 'CRITICAL': return 'badge-critical';
      case 'HIGH': return 'badge-high';
      case 'MODERATE': return 'badge-moderate';
      case 'LOW': return 'badge-low';
    }
  };

  return (
    <div>
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <div>
            <h3 className="card-title" style={{ margin: 0 }}>Comunicação e Gestão de Alertas</h3>
            <p style={{ color: '#64748b', fontSize: '0.85rem', marginTop: '4px' }}>
              Emissão autorizada de alertas Encosta Segura e histórico operacional
            </p>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.875rem', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={activeOnly}
                onChange={(e) => setActiveOnly(e.target.checked)}
              />
              Apenas Alertas Ativos
            </label>

            <button
              className="btn btn-primary"
              onClick={() => setShowCreateModal(true)}
            >
              🚨 Emitir Novo Alerta
            </button>
          </div>
        </div>

        {/* Table */}
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Título / Detalhes</th>
                <th>Severidade</th>
                <th>Categoria</th>
                <th>Origem</th>
                <th>Status</th>
                <th>Expira em</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '32px', color: '#64748b' }}>
                    Carregando alertas...
                  </td>
                </tr>
              ) : alerts.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '32px', color: '#64748b' }}>
                    Nenhum alerta cadastrado.
                  </td>
                </tr>
              ) : (
                alerts.map((alert) => (
                  <tr key={alert.id}>
                    <td>
                      <strong>{alert.title}</strong>
                      <p style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '2px', maxWidth: '320px' }}>
                        {alert.description}
                      </p>
                      {alert.recommendations && alert.recommendations.length > 0 && (
                        <div style={{ fontSize: '0.75rem', color: '#2563eb', marginTop: '4px' }}>
                          Recomendações: {alert.recommendations.join(' • ')}
                        </div>
                      )}
                    </td>
                    <td>
                      <span className={`badge ${getSeverityBadgeClass(alert.severity)}`}>
                        {alert.severity}
                      </span>
                    </td>
                    <td>{alert.category}</td>
                    <td><span className="badge badge-active">{alert.origin}</span></td>
                    <td>
                      <span className={`badge ${alert.status === 'ACTIVE' ? 'badge-critical' : 'badge-closed'}`}>
                        {alert.status}
                      </span>
                    </td>
                    <td style={{ fontSize: '0.8rem', whiteSpace: 'nowrap' }}>
                      {new Date(alert.expiresAt).toLocaleString()}
                    </td>
                    <td>
                      {alert.status === 'ACTIVE' && (
                        <button
                          className="btn btn-danger btn-sm"
                          onClick={() => setClosingAlert(alert)}
                        >
                          Encerrar
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal Emitir Alerta */}
      {showCreateModal && (
        <div className="modal-overlay">
          <div className="modal">
            <div className="modal-header">
              <h3 className="modal-title">Emitir Novo Alerta Encosta Segura</h3>
              <button
                style={{ background: 'none', border: 'none', fontSize: '1.2rem', cursor: 'pointer' }}
                onClick={() => setShowCreateModal(false)}
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleCreateAlert}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label" htmlFor="alert-title">Título do Alerta</label>
                  <input
                    id="alert-title"
                    type="text"
                    required
                    className="form-input"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="Ex: Alerta de Alagamento Iminente na Região Central"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="alert-desc">Descrição e Contexto</label>
                  <textarea
                    id="alert-desc"
                    required
                    className="form-textarea"
                    rows={3}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="Descreva a situação, ruas interditadas e riscos imediatos..."
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <div className="form-group">
                    <label className="form-label" htmlFor="alert-cat">Categoria do Evento</label>
                    <select
                      id="alert-cat"
                      className="form-select"
                      value={category}
                      onChange={(e) => setCategory(e.target.value as EventCategory)}
                    >
                      <option value="FLOODING">Alagamento (FLOODING)</option>
                      <option value="FLOOD">Inundação (FLOOD)</option>
                      <option value="LANDSLIDE">Deslizamento (LANDSLIDE)</option>
                      <option value="FLASH_FLOOD">Enxurrada (FLASH_FLOOD)</option>
                      <option value="HEAVY_RAIN">Chuva Intensa (HEAVY_RAIN)</option>
                      <option value="STORM_STRONG_WINDS">Vendaval (STORM)</option>
                      <option value="CONTAINMENT_FAILURE">Falha de Contenção</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="alert-sev">Severidade</label>
                    <select
                      id="alert-sev"
                      className="form-select"
                      value={severity}
                      onChange={(e) => setSeverity(e.target.value as AlertSeverity)}
                    >
                      <option value="LOW">Baixa (LOW)</option>
                      <option value="MODERATE">Moderada (MODERATE)</option>
                      <option value="HIGH">Alta (HIGH)</option>
                      <option value="CRITICAL">Crítica (CRITICAL)</option>
                    </select>
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="alert-exp">Tempo de Validade (Horas)</label>
                  <select
                    id="alert-exp"
                    className="form-select"
                    value={expiresAtHours}
                    onChange={(e) => setExpiresAtHours(e.target.value)}
                  >
                    <option value="2">2 Horas</option>
                    <option value="4">4 Horas (Padrão)</option>
                    <option value="8">8 Horas</option>
                    <option value="12">12 Horas</option>
                    <option value="24">24 Horas</option>
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="alert-recs">Recomendações para a População (uma por linha)</label>
                  <textarea
                    id="alert-recs"
                    className="form-textarea"
                    rows={3}
                    value={recommendationsText}
                    onChange={(e) => setRecommendationsText(e.target.value)}
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowCreateModal(false)}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={creating}
                >
                  {creating ? 'Transmitindo...' : 'Publicar Alerta Oficial'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Encerrar Alerta */}
      {closingAlert && (
        <div className="modal-overlay">
          <div className="modal">
            <div className="modal-header">
              <h3 className="modal-title">Encerrar Alerta #{closingAlert.id.slice(0, 8)}</h3>
              <button
                style={{ background: 'none', border: 'none', fontSize: '1.2rem', cursor: 'pointer' }}
                onClick={() => setClosingAlert(null)}
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleCloseAlert}>
              <div className="modal-body">
                <p style={{ fontSize: '0.875rem', color: '#64748b', marginBottom: '16px' }}>
                  Atenção: A legislação e as regras do Encosta Segura (RN033) exigem justificativa formal registrada em auditoria para o encerramento do alerta.
                </p>

                <div className="form-group">
                  <label className="form-label" htmlFor="close-reason">Motivo / Justificativa do Encerramento *</label>
                  <textarea
                    id="close-reason"
                    required
                    className="form-textarea"
                    rows={4}
                    value={closeReason}
                    onChange={(e) => setCloseReason(e.target.value)}
                    placeholder="Ex: Nível das águas retornou à cota segura e via desobstruída pela equipe de operações."
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setClosingAlert(null)}
                >
                  Voltar
                </button>
                <button
                  type="submit"
                  className="btn btn-danger"
                  disabled={closing}
                >
                  {closing ? 'Registrando...' : 'Confirmar Encerramento'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
