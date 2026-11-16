import React, { useState } from 'react';
import { useAuth } from './contexts/AuthContext';
import { LoginPage } from './pages/login/LoginPage';
import { AdminLayout } from './layouts/AdminLayout';
import { DashboardPage } from './pages/dashboard/DashboardPage';
import { OccurrencesPage } from './pages/occurrences/OccurrencesPage';
import { RiskAssessmentsPage } from './pages/risk-areas/RiskAssessmentsPage';
import { AlertsPage } from './pages/alerts/AlertsPage';
import { SafeLocationsPage } from './pages/safe-locations/SafeLocationsPage';

export const App: React.FC = () => {
  const { isAuthenticated } = useAuth();
  const [currentTab, setCurrentTab] = useState('dashboard');

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  const renderContent = () => {
    switch (currentTab) {
      case 'dashboard':
        return <DashboardPage onNavigate={setCurrentTab} />;
      case 'occurrences':
        return <OccurrencesPage />;
      case 'risk':
        return <RiskAssessmentsPage />;
      case 'alerts':
        return <AlertsPage />;
      case 'safe-locations':
        return <SafeLocationsPage />;
      default:
        return <DashboardPage onNavigate={setCurrentTab} />;
    }
  };

  return (
    <AdminLayout currentTab={currentTab} setCurrentTab={setCurrentTab}>
      {renderContent()}
    </AdminLayout>
  );
};
