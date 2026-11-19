import React, { useState } from 'react';
import { useMobileAuth } from '../../contexts/AuthContext';

interface LoginScreenProps {
  onContinueAsGuest: () => void;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({ onContinueAsGuest }) => {
  const { login } = useMobileAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(email, password);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Não foi possível autenticar. Verifique seus dados.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      maxWidth: '420px',
      margin: '0 auto',
      padding: '32px 20px',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'center',
      minHeight: '100vh',
    }}>
      <div style={{ textAlign: 'center', marginBottom: '32px' }}>
        <div style={{
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: '64px',
          height: '64px',
          borderRadius: '16px',
          backgroundColor: '#eff6ff',
          color: '#2563eb',
          marginBottom: '16px',
        }}>
          <svg width="36" height="36" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z"/>
          </svg>
        </div>
        <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: '#0f172a' }}>Encosta Segura</h1>
        <p style={{ color: '#64748b', fontSize: '0.95rem', marginTop: '6px' }}>
          Orientação urbana e monitoramento de riscos em tempo real
        </p>
      </div>

      {error && (
        <div style={{
          padding: '12px',
          borderRadius: '8px',
          backgroundColor: '#fef2f2',
          color: '#b91c1c',
          fontSize: '0.875rem',
          marginBottom: '20px',
          border: '1px solid #fecaca',
        }}>
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>
            E-mail
          </label>
          <input
            type="email"
            required
            style={{
              width: '100%',
              padding: '12px 14px',
              borderRadius: '8px',
              border: '1px solid #cbd5e1',
              fontSize: '1rem',
              outline: 'none',
              boxSizing: 'border-box',
            }}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="seu.email@exemplo.com"
          />
        </div>

        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>
            Senha
          </label>
          <input
            type="password"
            required
            style={{
              width: '100%',
              padding: '12px 14px',
              borderRadius: '8px',
              border: '1px solid #cbd5e1',
              fontSize: '1rem',
              outline: 'none',
              boxSizing: 'border-box',
            }}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••••••"
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          style={{
            padding: '14px',
            borderRadius: '10px',
            backgroundColor: '#2563eb',
            color: '#ffffff',
            border: 'none',
            fontSize: '1rem',
            fontWeight: 600,
            cursor: 'pointer',
            marginTop: '8px',
          }}
        >
          {loading ? 'Entrando...' : 'Entrar com Conta'}
        </button>

        <button
          type="button"
          onClick={onContinueAsGuest}
          style={{
            padding: '12px',
            borderRadius: '10px',
            backgroundColor: 'transparent',
            color: '#475569',
            border: '1px solid #cbd5e1',
            fontSize: '0.95rem',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          Continuar sem Login (Acesso Público)
        </button>
      </form>

      <div style={{ marginTop: '28px', borderTop: '1px solid #e2e8f0', paddingTop: '16px' }}>
        <button
          type="button"
          onClick={() => {
            setEmail('cidadao.demo@example.com');
            setPassword('SenhaSegura@123');
          }}
          style={{
            width: '100%',
            padding: '8px',
            fontSize: '0.8rem',
            backgroundColor: '#f1f5f9',
            border: 'none',
            borderRadius: '6px',
            color: '#334155',
            cursor: 'pointer',
          }}
        >
          Usar conta demo: cidadao.demo@example.com
        </button>
      </div>
    </div>
  );
};
