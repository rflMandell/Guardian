import React from 'react';
import { MobileAuthProvider } from './contexts/AuthContext';
import { AppNavigator } from './navigation/AppNavigator';

export const App: React.FC = () => {
  return (
    <MobileAuthProvider>
      <AppNavigator />
    </MobileAuthProvider>
  );
};

export default App;
