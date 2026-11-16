import React, { useEffect, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { api } from '../../services/api';
import { SafeLocation, SafeLocationOperationalStatus, SafeLocationType } from '../../types';

export const SafeLocationsPage: React.FC = () => {
  const { selectedMunicipalityId } = useAuth();
  const [locations, setLocations] = useState<SafeLocation[]>([]);
  const [loading, setLoading] = useState(false);
  const [availableOnly, setAvailableOnly] = useState(false);

  // Modal Novo Ponto Seguro
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [name, setName] = useState('');
  const [type, setType] = useState<SafeLocationType>('TEMPORARY_SHELTER');
  const [operationalStatus] = useState<SafeLocationOperationalStatus>('ACTIVE');
  const [capacity, setCapacity] = useState('150');
  const [latitude, setLatitude] = useState('-23.5505');
  const [longitude, setLongitude] = useState('-46.6333');
  const [address, setAddress] = useState('');
  const [contactPhone, setContactPhone] = useState('');
  const [creating, setCreating] = useState(false);

  // Modal Atualizar Disponibilidade
  const [selectedLoc, setSelectedLoc] = useState<SafeLocation | null>(null);
  const [editStatus, setEditStatus] = useState<SafeLocationOperationalStatus>('ACTIVE');
  const [editOccupancy, setEditOccupancy] = useState('');
  const [editCapacity, setEditCapacity] = useState('');
  const [updating, setUpdating] = useState(false);

  const loadLocations = () => {
    if (!selectedMunicipalityId) return;
    setLoading(true);
    api.listSafeLocations({
      municipalityId: selectedMunicipalityId,
      availableOnly: availableOnly || undefined,
      size: 50,
    })
      .then((res) => setLocations(res.data))
      .catch((err) => console.error('Erro ao buscar pontos seguros:', err))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadLocations();
  }, [selectedMunicipalityId, availableOnly]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedMunicipalityId) return;

    setCreating(true);
    try {
      await api.createSafeLocation({
        municipalityId: selectedMunicipalityId,
        name,
        type,
        operationalStatus,
        capacity: parseInt(capacity, 10),
        latitude: parseFloat(latitude),
        longitude: parseFloat(longitude),
        address,
        contactPhone,
        accessibilityFeatures: ['RAMP', 'ACCESSIBLE_BATHROOM'],
      });

      setShowCreateModal(false);
      setName('');
      setAddress('');
      loadLocations();
    } catch (err: unknown) {
      if (err instanceof Error) {
        alert(err.message);
      } else {
        alert('Erro ao cadastrar ponto seguro.');
      }
    } finally {
      setCreating(false);
    }
  };

  const handleUpdateAvailability = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLoc) return;

    setUpdating(true);
    try {
      await api.updateSafeLocationAvailability(selectedLoc.id, {
        operationalStatus: editStatus,
        capacity: editCapacity ? parseInt(editCapacity, 10) : undefined,
        currentOccupancy: editOccupancy ? parseInt(editOccupancy, 10) : undefined,
        notes: 'Atualização periódica de capacidade via painel operacional',
      });

      setSelectedLoc(null);
      loadLocations();
    } catch (err: unknown) {
      if (err instanceof Error) {
        alert(err.message);
      } else {
        alert('Erro ao atualizar disponibilidade.');
      }
    } finally {
      setUpdating(false);
    }
  };

  const getAvailabilityBadgeClass = (avail: string) => {
    switch (avail) {
      case 'AVAILABLE': return 'badge-available';
      case 'LIMITED': return 'badge-moderate';
      case 'FULL': return 'badge-critical';
      default: return 'badge-unavailable';
    }
  };

  return (
    <div>
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <div>
            <h3 className="card-title" style={{ margin: 0 }}>Pontos Seguros e Abrigos de Acolhimento</h3>
            <p style={{ color: '#64748b', fontSize: '0.85rem', marginTop: '4px' }}>
              Gestão de capacidade, infraestrutura de acolhimento e disponibilidade em tempo real
            </p>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.875rem', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={availableOnly}
                onChange={(e) => setAvailableOnly(e.target.checked)}
              />
              Apenas Disponíveis (Capacidade Livre)
            </label>

            <button
              className="btn btn-primary"
              onClick={() => setShowCreateModal(true)}
            >
              ➕ Cadastrar Ponto Seguro
            </button>
          </div>
        </div>

        {/* Table */}
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Nome / Endereço</th>
                <th>Tipo</th>
                <th>Status Operacional</th>
                <th>Disponibilidade</th>
                <th>Ocupação / Capacidade</th>
                <th>Contato</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '32px', color: '#64748b' }}>
                    Carregando pontos seguros...
                  </td>
                </tr>
              ) : locations.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '32px', color: '#64748b' }}>
                    Nenhum ponto seguro encontrado.
                  </td>
                </tr>
              ) : (
                locations.map((loc) => {
                  const cap = loc.capacity ?? 0;
                  const occ = loc.currentOccupancy ?? 0;
                  const percent = cap > 0 ? Math.round((occ / cap) * 100) : 0;

                  return (
                    <tr key={loc.id}>
                      <td>
                        <strong>{loc.name}</strong>
                        {loc.address && (
                          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '2px' }}>
                            {loc.address}
                          </div>
                        )}
                      </td>
                      <td><span className="badge badge-active">{loc.type}</span></td>
                      <td>{loc.operationalStatus}</td>
                      <td>
                        <span className={`badge ${getAvailabilityBadgeClass(loc.availability)}`}>
                          {loc.availability}
                        </span>
                      </td>
                      <td>
                        <div style={{ fontWeight: 600 }}>
                          {occ} / {cap} ({percent}%)
                        </div>
                        <div style={{
                          width: '100px',
                          height: '6px',
                          backgroundColor: '#e2e8f0',
                          borderRadius: '3px',
                          overflow: 'hidden',
                          marginTop: '4px'
                        }}>
                          <div style={{
                            width: `${Math.min(percent, 100)}%`,
                            height: '100%',
                            backgroundColor: percent >= 90 ? '#ef4444' : percent >= 70 ? '#f59e0b' : '#10b981'
                          }} />
                        </div>
                      </td>
                      <td style={{ fontSize: '0.8rem' }}>{loc.contactPhone || '—'}</td>
                      <td>
                        <button
                          className="btn btn-secondary btn-sm"
                          onClick={() => {
                            setSelectedLoc(loc);
                            setEditStatus(loc.operationalStatus);
                            setEditOccupancy(loc.currentOccupancy?.toString() || '');
                            setEditCapacity(loc.capacity?.toString() || '');
                          }}
                        >
                          Lotação / Status
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal Criar Ponto Seguro */}
      {showCreateModal && (
        <div className="modal-overlay">
          <div className="modal">
            <div className="modal-header">
              <h3 className="modal-title">Novo Ponto Seguro / Abrigo</h3>
              <button
                style={{ background: 'none', border: 'none', fontSize: '1.2rem', cursor: 'pointer' }}
                onClick={() => setShowCreateModal(false)}
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleCreate}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label" htmlFor="safe-name">Nome do Local</label>
                  <input
                    id="safe-name"
                    type="text"
                    required
                    className="form-input"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Ex: Ginásio Municipal de Esportes Santos"
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <div className="form-group">
                    <label className="form-label" htmlFor="safe-type">Tipo de Instalação</label>
                    <select
                      id="safe-type"
                      className="form-select"
                      value={type}
                      onChange={(e) => setType(e.target.value as SafeLocationType)}
                    >
                      <option value="TEMPORARY_SHELTER">Abrigo Temporário</option>
                      <option value="COMMUNITY_CENTER">Centro Comunitário</option>
                      <option value="PUBLIC_SCHOOL">Escola Pública</option>
                      <option value="SPORTS_ARENA">Arena / Ginásio</option>
                      <option value="HEALTH_UNIT">Unidade de Saúde</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="safe-cap">Capacidade Máxima de Pessoas</label>
                    <input
                      id="safe-cap"
                      type="number"
                      required
                      min={1}
                      className="form-input"
                      value={capacity}
                      onChange={(e) => setCapacity(e.target.value)}
                    />
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <div className="form-group">
                    <label className="form-label" htmlFor="safe-lat">Latitude</label>
                    <input
                      id="safe-lat"
                      type="number"
                      step="any"
                      required
                      className="form-input"
                      value={latitude}
                      onChange={(e) => setLatitude(e.target.value)}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="safe-lon">Longitude</label>
                    <input
                      id="safe-lon"
                      type="number"
                      step="any"
                      required
                      className="form-input"
                      value={longitude}
                      onChange={(e) => setLongitude(e.target.value)}
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="safe-addr">Endereço Completo</label>
                  <input
                    id="safe-addr"
                    type="text"
                    className="form-input"
                    value={address}
                    onChange={(e) => setAddress(e.target.value)}
                    placeholder="Rua, número, bairro..."
                  />
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="safe-phone">Telefone de Contato</label>
                  <input
                    id="safe-phone"
                    type="text"
                    className="form-input"
                    value={contactPhone}
                    onChange={(e) => setContactPhone(e.target.value)}
                    placeholder="(11) 98765-4321"
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
                  {creating ? 'Cadastrando...' : 'Cadastrar Ponto Seguro'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Atualizar Lotação */}
      {selectedLoc && (
        <div className="modal-overlay">
          <div className="modal">
            <div className="modal-header">
              <h3 className="modal-title">Atualizar Lotação — {selectedLoc.name}</h3>
              <button
                style={{ background: 'none', border: 'none', fontSize: '1.2rem', cursor: 'pointer' }}
                onClick={() => setSelectedLoc(null)}
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleUpdateAvailability}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label" htmlFor="edit-status">Status Operacional</label>
                  <select
                    id="edit-status"
                    className="form-select"
                    value={editStatus}
                    onChange={(e) => setEditStatus(e.target.value as SafeLocationOperationalStatus)}
                  >
                    <option value="ACTIVE">Ativo (Aberto para Acolhimento)</option>
                    <option value="UNDER_MAINTENANCE">Em Manutenção</option>
                    <option value="CLOSED">Fechado / Interditado</option>
                    <option value="INACTIVE">Inativo</option>
                  </select>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <div className="form-group">
                    <label className="form-label" htmlFor="edit-occ">Pessoas Acolhidas (Ocupação Atual)</label>
                    <input
                      id="edit-occ"
                      type="number"
                      min={0}
                      className="form-input"
                      value={editOccupancy}
                      onChange={(e) => setEditOccupancy(e.target.value)}
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="edit-cap">Capacidade Total</label>
                    <input
                      id="edit-cap"
                      type="number"
                      min={1}
                      className="form-input"
                      value={editCapacity}
                      onChange={(e) => setEditCapacity(e.target.value)}
                    />
                  </div>
                </div>

                <p style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '8px' }}>
                  Regra RN042: O sistema recalcula automaticamente a disponibilidade. Se o local estiver fechado ou com ocupação total, será apresentado como indisponível para os cidadãos.
                </p>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setSelectedLoc(null)}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={updating}
                >
                  {updating ? 'Salvando...' : 'Atualizar Lotação'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
