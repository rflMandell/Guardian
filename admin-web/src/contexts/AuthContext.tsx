import React, { createContext, useContext, useState, useEffect } from 'react';
import { UserSummary, UserRole } from '../types';
import { api } from '../services/api';

interface AuthContextType {
  user: UserSummary | null;
  isAuthenticated: boolean;
  selectedMunicipalityId: string | null;
  setSelectedMunicipalityId: (id: string | null) => void;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  hasRole: (roles: UserRole[]) => boolean;
  hasPermission: (permission: string) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserSummary | null>(() => {
    const saved = localStorage.getItem('guardian_user');
    return saved ? JSON.parse(saved) : null;
  });

  const [selectedMunicipalityId, setSelectedMunicipalityId] = useState<string | null>(() => {
    if (user?.municipalityId) return user.municipalityId;
    return localStorage.getItem('guardian_selected_mun') || null;
  });

  useEffect(() => {
    const handleExpired = () => {
      setUser(null);
    };
    window.addEventListener('guardian_auth_expired', handleExpired);
    return () => window.removeEventListener('guardian_auth_expired', handleExpired);
  }, []);

  const login = async (email: string, password: string) => {
    const data = await api.login(email, password);
    setUser(data.user);
    if (data.user.municipalityId) {
      setSelectedMunicipalityId(data.user.municipalityId);
      localStorage.setItem('guardian_selected_mun', data.user.municipalityId);
    }
  };

  const logout = () => {
    setUser(null);
    setSelectedMunicipalityId(null);
    localStorage.removeItem('guardian_selected_mun');
    api.logout();
  };

  const handleSelectMunicipality = (id: string | null) => {
    setSelectedMunicipalityId(id);
    if (id) {
      localStorage.setItem('guardian_selected_mun', id);
    } else {
      localStorage.removeItem('guardian_selected_mun');
    }
  };

  const hasRole = (roles: UserRole[]) => {
    if (!user) return false;
    return roles.includes(user.role);
  };

  const hasPermission = (permission: string) => {
    if (!user) return false;
    if (user.role === 'ROLE_ADMIN') return true;
    return user.permissions?.includes(permission) ?? false;
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        selectedMunicipalityId,
        setSelectedMunicipalityId: handleSelectMunicipality,
        login,
        logout,
        hasRole,
        hasPermission,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth deve ser utilizado dentro de um AuthProvider');
  }
  return context;
};
