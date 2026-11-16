import React, { useState } from 'react';
import { useMobileAuth } from '../contexts/AuthContext';
import { LoginScreen } from '../screens/auth/LoginScreen';
import { HomeScreen } from '../screens/home/HomeScreen';
import { RiskScreen } from '../screens/risk/RiskScreen';
import { AlertsScreen } from '../screens/alerts/AlertsScreen';
import { CreateOccurrenceScreen } from '../screens/occurrences/CreateOccurrenceScreen';
import { SafeLocationsScreen } from '../screens/safe-locations/SafeLocationsScreen';

export const AppNavigator: React.FC = () => {
  const { isAuthenticated } = useMobileAuth();
  const [activeTab, setActiveTab] = useState('home');
  const [guestMode, setGuestMode] = useState(false);

  if (!isAuthenticated && !guestMode) {
    return (
      <LoginScreen
        onContinueAsGuest={() => setGuestMode(true)}
      />
    );
  }

  const renderScreen = () => {
    switch (activeTab) {
      case 'home':
        return <HomeScreen onNavigate={setActiveTab} />;
      case 'risk':
        return <RiskScreen />;
      case 'occurrences':
        return <CreateOccurrenceScreen onSuccess={() => setActiveTab('home')} />;
      case 'alerts':
        return <AlertsScreen />;
      case 'safe-locations':
        return <SafeLocationsScreen />;
      default:
        return <HomeScreen onNavigate={setActiveTab} />;
    }
  };

  const navItems = [
    { id: 'home', label: 'Início', icon: '🏠' },
    { id: 'risk', label: 'Riscos', icon: '⚠️' },
    { id: 'occurrences', label: 'Relatar', icon: '➕' },
    { id: 'alerts', label: 'Alertas', icon: '🚨' },
    { id: 'safe-locations', label: 'Abrigos', icon: '🛡️' },
  ];

  return (
    <div style={{
      backgroundColor: '#f8fafc',
      minHeight: '100vh',
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
      position: 'relative',
    }}>
      {renderScreen()}

      {/* Bottom Navigation Bar */}
      <nav style={{
        position: 'fixed',
        bottom: 0,
        left: 0,
        right: 0,
        height: '65px',
        backgroundColor: '#ffffff',
        borderTop: '1px solid #e2e8f0',
        display: 'flex',
        justifyContent: 'space-around',
        alignItems: 'center',
        zIndex: 100,
        boxShadow: '0 -2px 10px rgba(0,0,0,0.05)',
      }}>
        {navItems.map((item) => {
          const isActive = activeTab === item.id;

          return (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id)}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '4px',
                border: 'none',
                backgroundColor: 'transparent',
                cursor: 'pointer',
                color: isActive ? '#2563eb' : '#64748b',
                fontWeight: isActive ? 700 : 500,
                fontSize: '0.75rem',
                flex: 1,
              }}
            >
              <span style={{ fontSize: '1.25rem' }}>{item.icon}</span>
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>
    </div>
  );
};
