import React, { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { api } from '../services/api';
import { Municipality } from '../types';

interface AdminLayoutProps {
  currentTab: string;
  setCurrentTab: (tab: string) => void;
  children: React.ReactNode;
}

export const AdminLayout: React.FC<AdminLayoutProps> = ({ currentTab, setCurrentTab, children }) => {
  const { user, logout, selectedMunicipalityId, setSelectedMunicipalityId } = useAuth();
  const [municipalities, setMunicipalities] = useState<Municipality[]>([]);

  useEffect(() => {
    api.listMunicipalities()
      .then((data) => {
        setMunicipalities(data);
        if (!selectedMunicipalityId && data.length > 0) {
          setSelectedMunicipalityId(data[0].id);
        }
      })
      .catch((err) => console.error('Erro ao carregar municípios:', err));
  }, [selectedMunicipalityId, setSelectedMunicipalityId]);

  const getRoleLabel = (role?: string) => {
    switch (role) {
      case 'ROLE_ADMIN':
        return 'Administrador Global';
      case 'ROLE_DEFESA_CIVIL':
        return 'Defesa Civil';
      case 'ROLE_OPERATOR':
        return 'Operador Municipal';
      default:
        return 'Cidadão';
    }
  };

  const navItems = [
    { id: 'dashboard', label: 'Visão Geral / Dashboard', icon: '📊' },
    { id: 'occurrences', label: 'Relatos de Ocorrência', icon: '📝' },
    { id: 'risk', label: 'Avaliação de Risco', icon: '⚠️' },
    { id: 'alerts', label: 'Gestão de Alertas', icon: '🚨' },
    { id: 'safe-locations', label: 'Pontos Seguros / Abrigos', icon: '🛡️' },
  ];

  return (
    <div className="app-layout">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="sidebar-brand">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="#38bdf8">
            <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z"/>
          </svg>
          <div>
            <h1>Encosta Segura</h1>
            <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Painel Administrativo</span>
          </div>
        </div>

        <nav className="sidebar-nav">
          {navItems.map((item) => (
            <button
              key={item.id}
              className={`nav-item ${currentTab === item.id ? 'active' : ''}`}
              onClick={() => setCurrentTab(item.id)}
            >
              <span style={{ fontSize: '1.1rem' }}>{item.icon}</span>
              <span>{item.label}</span>
            </button>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="user-badge">
            <span className="user-name">{user?.name}</span>
            <span className="user-role">{getRoleLabel(user?.role)}</span>
          </div>
          <button
            onClick={logout}
            className="btn btn-secondary btn-sm"
            style={{ width: '100%', marginTop: '8px', color: '#ef4444', borderColor: '#334155' }}
          >
            Encerrar Sessão
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="main-content">
        <header className="header">
          <div className="header-title">
            {navItems.find((n) => n.id === currentTab)?.label || 'Painel'}
          </div>

          <div className="header-actions">
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <label htmlFor="municipality-select" style={{ fontSize: '0.85rem', color: '#64748b', fontWeight: 500 }}>
                Município Ativo:
              </label>
              <select
                id="municipality-select"
                className="form-select"
                style={{ padding: '6px 12px', fontSize: '0.85rem', minWidth: '200px' }}
                value={selectedMunicipalityId || ''}
                onChange={(e) => setSelectedMunicipalityId(e.target.value)}
                disabled={user?.role !== 'ROLE_ADMIN' && !!user?.municipalityId}
              >
                {municipalities.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name} — {m.state}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </header>

        <main className="page-container">{children}</main>
      </div>
    </div>
  );
};
