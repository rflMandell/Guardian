import React, { useEffect, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { api } from '../../services/api';
import { OccurrenceReport, ReportStatus } from '../../types';

export const OccurrencesPage: React.FC = () => {
  const { selectedMunicipalityId } = useAuth();
  const [occurrences, setOccurrences] = useState<OccurrenceReport[]>([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [categoryFilter, setCategoryFilter] = useState<string>('');
  
  // Modal de edição de status
  const [selectedOcc, setSelectedOcc] = useState<OccurrenceReport | null>(null);
  const [newStatus, setNewStatus] = useState<ReportStatus>('UNDER_ANALYSIS');
  const [adminNotes, setAdminNotes] = useState('');
  const [updating, setUpdating] = useState(false);

  const loadOccurrences = () => {
    if (!selectedMunicipalityId) return;
    setLoading(true);
    api.listOccurrences({
      municipalityId: selectedMunicipalityId,
      status: statusFilter || undefined,
      category: categoryFilter || undefined,
      size: 50,
    })
      .then((res) => setOccurrences(res.data))
      .catch((err) => console.error('Erro ao buscar ocorrências:', err))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadOccurrences();
  }, [selectedMunicipalityId, statusFilter, categoryFilter]);

  const handleUpdateStatus = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedOcc) return;

    setUpdating(true);
    try {
      await api.updateOccurrenceStatus(selectedOcc.id, newStatus, adminNotes);
      setSelectedOcc(null);
      setAdminNotes('');
      loadOccurrences();
    } catch (err: unknown) {
      if (err instanceof Error) {
        alert(err.message);
      } else {
        alert('Erro ao atualizar ocorrência.');
      }
    } finally {
      setUpdating(false);
    }
  };

  const getStatusBadgeClass = (status: ReportStatus) => {
    switch (status) {
      case 'RECEIVED': return 'badge-received';
      case 'UNDER_ANALYSIS': return 'badge-active';
      case 'LINKED_TO_INCIDENT': return 'badge-critical';
      case 'DISMISSED': return 'badge-closed';
      case 'CLOSED': return 'badge-low';
      default: return 'badge-closed';
    }
  };

  return (
    <div>
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <div>
            <h3 className="card-title" style={{ margin: 0 }}>Triagem e Gestão de Ocorrências</h3>
            <p style={{ color: '#64748b', fontSize: '0.85rem', marginTop: '4px' }}>
              Relatos submetidos pela população e canais oficiais de monitoramento
            </p>
          </div>

          {/* Filters */}
          <div style={{ display: 'flex', gap: '12px' }}>
            <select
              className="form-select"
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
            >
              <option value="">Todas as Categorias</option>
              <option value="FLOODING">Alagamento (FLOODING)</option>
              <option value="FLOOD">Inundação (FLOOD)</option>
              <option value="LANDSLIDE">Deslizamento (LANDSLIDE)</option>
              <option value="FLASH_FLOOD">Enxurrada (FLASH_FLOOD)</option>
              <option value="HEAVY_RAIN">Chuva Intensa (HEAVY_RAIN)</option>
              <option value="STORM_STRONG_WINDS">Vendaval (STORM)</option>
              <option value="CONTAINMENT_FAILURE">Falha de Contenção</option>
            </select>

            <select
              className="form-select"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">Todos os Status</option>
              <option value="RECEIVED">Recebido (RECEIVED)</option>
              <option value="UNDER_ANALYSIS">Em Análise (UNDER_ANALYSIS)</option>
              <option value="LINKED_TO_INCIDENT">Incidente Confirmado</option>
              <option value="DISMISSED">Descartado (DISMISSED)</option>
              <option value="CLOSED">Encerrado (CLOSED)</option>
            </select>

            <button className="btn btn-secondary" onClick={loadOccurrences}>
              Atualizar
            </button>
          </div>
        </div>

        {/* Table */}
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Data / Hora</th>
                <th>Categoria</th>
                <th>Descrição do Relato</th>
                <th>Coordenadas / Local</th>
                <th>Status</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '32px', color: '#64748b' }}>
                    Carregando ocorrências...
                  </td>
                </tr>
              ) : occurrences.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '32px', color: '#64748b' }}>
                    Nenhuma ocorrência encontrada para os filtros selecionados.
                  </td>
                </tr>
              ) : (
                occurrences.map((occ) => (
                  <tr key={occ.id}>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      {new Date(occ.createdAt).toLocaleDateString()} {new Date(occ.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </td>
                    <td>
                      <strong>{occ.category}</strong>
                    </td>
                    <td style={{ maxWidth: '320px' }}>
                      <div style={{ wordBreak: 'break-word' }}>{occ.description}</div>
                      {occ.addressReference && (
                        <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '2px' }}>
                          Ref: {occ.addressReference}
                        </div>
                      )}
                    </td>
                    <td style={{ fontSize: '0.8rem', color: '#334155' }}>
                      Lat: {occ.latitude.toFixed(4)}<br />
                      Lon: {occ.longitude.toFixed(4)}
                    </td>
                    <td>
                      <span className={`badge ${getStatusBadgeClass(occ.status)}`}>
                        {occ.status}
                      </span>
                    </td>
                    <td>
                      <button
                        className="btn btn-secondary btn-sm"
                        onClick={() => {
                          setSelectedOcc(occ);
                          setNewStatus(occ.status);
                        }}
                      >
                        Avaliar / Alterar
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal de Atualização de Status */}
      {selectedOcc && (
        <div className="modal-overlay">
          <div className="modal">
            <div className="modal-header">
              <h3 className="modal-title">Atualizar Status da Ocorrência</h3>
              <button
                style={{ background: 'none', border: 'none', fontSize: '1.2rem', cursor: 'pointer' }}
                onClick={() => setSelectedOcc(null)}
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleUpdateStatus}>
              <div className="modal-body">
                <p style={{ fontSize: '0.85rem', color: '#64748b', marginBottom: '16px' }}>
                  Ocorrência <b>#{selectedOcc.id.slice(0, 8)}</b> — Categoria: <b>{selectedOcc.category}</b>
                </p>

                <div className="form-group">
                  <label className="form-label" htmlFor="status-select">Novo Status Operacional</label>
                  <select
                    id="status-select"
                    className="form-select"
                    value={newStatus}
                    onChange={(e) => setNewStatus(e.target.value as ReportStatus)}
                  >
                    <option value="RECEIVED">Recebido (Aguardando)</option>
                    <option value="UNDER_ANALYSIS">Em Análise / Vistoria</option>
                    <option value="LINKED_TO_INCIDENT">Incidente Confirmado</option>
                    <option value="DISMISSED">Descartado / Trote / Improcedente</option>
                    <option value="CLOSED">Encerrado / Resolvido</option>
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="admin-notes">Parecer Técnico / Observações da Defesa Civil</label>
                  <textarea
                    id="admin-notes"
                    className="form-textarea"
                    rows={4}
                    value={adminNotes}
                    onChange={(e) => setAdminNotes(e.target.value)}
                    placeholder="Descreva as medidas tomadas ou a justificativa da decisão..."
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setSelectedOcc(null)}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={updating}
                >
                  {updating ? 'Gravando...' : 'Confirmar Atualização'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
