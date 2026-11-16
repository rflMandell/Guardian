import { appStorage } from '../storage';
import {
  LoginResponseData,
  AuthTokens,
  UserSummary,
  OccurrenceReport,
  RiskAssessment,
  Alert,
  SafeLocation,
  Municipality,
  GeolocationStatus,
} from '../types';

const API_BASE_URL = typeof window !== 'undefined' && window.location.port === '3000'
  ? '/api/v1'
  : 'http://localhost:8080/api/v1';

class MobileApiClient {
  private async getAuthHeader(): Promise<Record<string, string>> {
    const token = await appStorage.getItem('guardian_mobile_token');
    if (!token) return {};
    return { Authorization: `Bearer ${token}` };
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const authHeaders = await this.getAuthHeader();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...authHeaders,
      ...(options.headers as Record<string, string>),
    };

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    const contentType = response.headers.get('content-type');
    const isJson = contentType && contentType.includes('application/json');
    const data = isJson ? await response.json() : null;

    if (!response.ok) {
      const errorMessage =
        data?.message ||
        data?.error ||
        `Falha na requisição (${response.status}): ${response.statusText}`;
      throw new Error(errorMessage);
    }

    return data as T;
  }

  // --- Auth ---
  async login(email: string, password: string): Promise<LoginResponseData> {
    const res = await this.request<any>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    const rawData = res.data;
    const accessToken = rawData.tokens?.accessToken || rawData.accessToken;
    const refreshToken = rawData.tokens?.refreshToken || rawData.refreshToken;
    const tokenType = rawData.tokens?.tokenType || rawData.tokenType || 'Bearer';
    const expiresIn = rawData.tokens?.expiresIn || rawData.expiresIn || 86400;

    const rawUser = rawData.user || {};
    const user: UserSummary = {
      id: rawUser.id,
      name: rawUser.name,
      email: rawUser.email || email,
      role: rawUser.role || (rawUser.roles && rawUser.roles[0]) || 'CITIZEN',
      municipalityId: rawUser.municipalityId || null,
    };

    const tokens: AuthTokens = {
      accessToken,
      refreshToken,
      tokenType,
      expiresIn,
    };

    await appStorage.setItem('guardian_mobile_token', accessToken);
    await appStorage.setItem('guardian_mobile_user', JSON.stringify(user));

    return {
      tokens,
      user,
    };
  }

  async logout(): Promise<void> {
    await appStorage.removeItem('guardian_mobile_token');
    await appStorage.removeItem('guardian_mobile_user');
  }

  // --- Municipalities ---
  async getMunicipalities(): Promise<Municipality[]> {
    const res = await this.request<{ data: Municipality[] }>('/municipalities?size=50');
    return res.data;
  }

  // --- Geolocation Status ---
  async getGeolocationStatus(lat: number, lon: number): Promise<GeolocationStatus> {
    try {
      const res = await this.request<{ data: GeolocationStatus }>(
        `/geolocation/status?latitude=${lat}&longitude=${lon}`
      );
      return res.data;
    } catch {
      // Fallback gracioso caso serviço de boundary esteja sem PostGIS ativo
      return {
        withinCoverage: true,
        inRiskArea: false,
        activeAlertsCount: 0,
      };
    }
  }

  // --- Risk ---
  async getLatestRisk(municipalityId: string): Promise<RiskAssessment> {
    const res = await this.request<{ data: RiskAssessment }>(
      `/risk-assessments/latest?municipalityId=${municipalityId}`
    );
    return res.data;
  }

  // --- Alerts ---
  async getActiveAlerts(municipalityId: string): Promise<Alert[]> {
    const res = await this.request<{ data: Alert[] }>(
      `/alerts?municipalityId=${municipalityId}&activeOnly=true&size=20`
    );
    return res.data;
  }

  // --- Occurrences ---
  async createOccurrence(payload: {
    municipalityId: string;
    category: string;
    description: string;
    latitude: number;
    longitude: number;
    addressReference?: string;
    occurredAt: string;
  }): Promise<OccurrenceReport> {
    const res = await this.request<{ data: OccurrenceReport }>('/occurrences', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    return res.data;
  }

  async getMyOccurrences(municipalityId: string): Promise<OccurrenceReport[]> {
    const res = await this.request<{ data: OccurrenceReport[] }>(
      `/occurrences?municipalityId=${municipalityId}&size=20`
    );
    return res.data;
  }

  // --- Safe Locations ---
  async getSafeLocations(municipalityId: string, availableOnly = true): Promise<SafeLocation[]> {
    const res = await this.request<{ data: SafeLocation[] }>(
      `/safe-locations?municipalityId=${municipalityId}&availableOnly=${availableOnly}&size=50`
    );
    return res.data;
  }
}

export const mobileApi = new MobileApiClient();
