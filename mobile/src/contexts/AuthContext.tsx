import React, { createContext, useContext, useState, useEffect } from 'react';
import { UserSummary } from '../types';
import { mobileApi } from '../services/api';
import { appStorage } from '../storage';

interface MobileAuthContextType {
  user: UserSummary | null;
  isAuthenticated: boolean;
  currentMunicipalityId: string | null;
  setCurrentMunicipalityId: (id: string | null) => void;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const MobileAuthContext = createContext<MobileAuthContextType | undefined>(undefined);

export const MobileAuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [currentMunicipalityId, setCurrentMunicipalityId] = useState<string | null>(null);

  useEffect(() => {
    appStorage.getItem('encosta_segura_mobile_user').then((saved) => {
      if (saved) {
        try {
          const parsed = JSON.parse(saved);
          setUser(parsed);
          if (parsed.municipalityId) {
            setCurrentMunicipalityId(parsed.municipalityId);
          }
        } catch {
          // ignore
        }
      }
    });
  }, []);

  const login = async (email: string, pass: string) => {
    const data = await mobileApi.login(email, pass);
    setUser(data.user);
    if (data.user.municipalityId) {
      setCurrentMunicipalityId(data.user.municipalityId);
    }
  };

  const logout = async () => {
    await mobileApi.logout();
    setUser(null);
  };

  return (
    <MobileAuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        currentMunicipalityId,
        setCurrentMunicipalityId,
        login,
        logout,
      }}
    >
      {children}
    </MobileAuthContext.Provider>
  );
};

export const useMobileAuth = () => {
  const ctx = useContext(MobileAuthContext);
  if (!ctx) {
    throw new Error('useMobileAuth deve ser utilizado dentro de MobileAuthProvider');
  }
  return ctx;
};
