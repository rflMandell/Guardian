import React, { useState, useEffect } from 'react';
import { useMobileAuth } from '../../contexts/AuthContext';
import { mobileApi } from '../../services/api';
import { getCurrentLocation, Coordinates } from '../../utils/geolocation';
import { EventCategory } from '../../types';

interface CreateOccurrenceScreenProps {
  onSuccess: () => void;
}

export const CreateOccurrenceScreen: React.FC<CreateOccurrenceScreenProps> = ({ onSuccess }) => {
  const { currentMunicipalityId } = useMobileAuth();
  const [coords, setCoords] = useState<Coordinates | null>(null);
  const [category, setCategory] = useState<EventCategory>('FLOODING');
  const [description, setDescription] = useState('');
  const [addressReference, setAddressReference] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    getCurrentLocation().then((loc) => setCoords(loc));
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentMunicipalityId) {
      setError('Município não identificado para envio.');
      return;
    }
    if (!coords) {
      setError('Aguardando coordenadas GPS do dispositivo.');
      return;
    }

    setError(null);
    setSubmitting(true);
    try {
      await mobileApi.createOccurrence({
        municipalityId: currentMunicipalityId,
        category,
        description,
        latitude: coords.latitude,
        longitude: coords.longitude,
        addressReference: addressReference.trim() || undefined,
        occurredAt: new Date().toISOString(),
      });

      setSuccess(true);
      setTimeout(() => {
        onSuccess();
      }, 2000);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Erro ao enviar relato de ocorrência.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (success) {
    return (
      <div style={{ padding: '40px 20px', textAlign: 'center', maxWidth: '420px', margin: '0 auto' }}>
        <span style={{ fontSize: '3rem' }}>🎉</span>
        <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: '#0f172a', marginTop: '16px' }}>
          Relato Enviado com Sucesso!
        </h2>
        <p style={{ color: '#64748b', fontSize: '0.9rem', marginTop: '8px' }}>
          A Defesa Civil e os operadores municipais do Guardian foram notificados para triagem imediata.
        </p>
      </div>
    );
  }

  return (
    <div style={{ padding: '20px', maxWidth: '500px', margin: '0 auto', paddingBottom: '90px' }}>
      <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: '#0f172a', marginBottom: '8px' }}>
        Registrar Ocorrência Urbana
      </h2>
      <p style={{ color: '#64748b', fontSize: '0.85rem', marginBottom: '20px' }}>
        Ajude o monitoramento da sua cidade informando pontos de alagamento, quedas de barreiras ou vias bloqueadas.
      </p>

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
        {/* Localização GPS */}
        <div style={{
          padding: '12px 16px',
          backgroundColor: '#f8fafc',
          borderRadius: '10px',
          border: '1px solid #e2e8f0',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}>
          <div>
            <span style={{ fontSize: '0.75rem', color: '#64748b', display: 'block' }}>LOCALIZAÇÃO DO EVENTO</span>
            <strong style={{ fontSize: '0.85rem', color: '#0f172a' }}>
              {coords ? `${coords.latitude.toFixed(4)}, ${coords.longitude.toFixed(4)}` : 'Obtendo GPS...'}
            </strong>
          </div>
          <span style={{ fontSize: '1.2rem' }}>📍</span>
        </div>

        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>
            Tipo de Evento *
          </label>
          <select
            style={{
              width: '100%',
              padding: '12px 14px',
              borderRadius: '8px',
              border: '1px solid #cbd5e1',
              fontSize: '0.95rem',
              backgroundColor: '#ffffff',
              outline: 'none',
              boxSizing: 'border-box',
            }}
            value={category}
            onChange={(e) => setCategory(e.target.value as EventCategory)}
          >
            <option value="FLOODING">Alagamento de Via Pública</option>
            <option value="FLOOD">Inundação de Rio / Córrego</option>
            <option value="LANDSLIDE">Deslizamento de Encosta / Terra</option>
            <option value="FLASH_FLOOD">Enxurrada Violenta</option>
            <option value="HEAVY_RAIN">Chuva Torrencial / Granizo</option>
            <option value="STORM_STRONG_WINDS">Vendaval com Queda de Árvores</option>
            <option value="CONTAINMENT_FAILURE">Rompimento de Dique ou Muro</option>
          </select>
        </div>

        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>
            Descrição da Situação *
          </label>
          <textarea
            required
            rows={4}
            style={{
              width: '100%',
              padding: '12px 14px',
              borderRadius: '8px',
              border: '1px solid #cbd5e1',
              fontSize: '0.95rem',
              outline: 'none',
              boxSizing: 'border-box',
            }}
            placeholder="Ex: Água subindo rápido cobrindo a calçada em frente à praça..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>

        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>
            Ponto de Referência (Opcional)
          </label>
          <input
            type="text"
            style={{
              width: '100%',
              padding: '12px 14px',
              borderRadius: '8px',
              border: '1px solid #cbd5e1',
              fontSize: '0.95rem',
              outline: 'none',
              boxSizing: 'border-box',
            }}
            placeholder="Ex: Próximo à ponte central ou ao supermercado"
            value={addressReference}
            onChange={(e) => setAddressReference(e.target.value)}
          />
        </div>

        <button
          type="submit"
          disabled={submitting}
          style={{
            padding: '14px',
            borderRadius: '10px',
            backgroundColor: '#2563eb',
            color: '#ffffff',
            border: 'none',
            fontSize: '1rem',
            fontWeight: 700,
            cursor: 'pointer',
            marginTop: '8px',
          }}
        >
          {submitting ? 'Registrando Relato...' : 'Enviar Relato à Defesa Civil'}
        </button>
      </form>
    </div>
  );
};
