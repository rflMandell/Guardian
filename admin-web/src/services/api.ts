import {
  ApiResponse,
  PaginatedResponse,
  LoginResponseData,
  AuthTokens,
  UserSummary,
  UserRole,
  OccurrenceReport,
  RiskAssessment,
  Alert,
  SafeLocation,
  Municipality,
  ReportStatus,
  SafeLocationOperationalStatus,
} from '../types';

function mapRole(roles?: string[]): UserRole {
  if (!roles || roles.length === 0) return 'ROLE_CITIZEN';
  if (roles.includes('GLOBAL_ADMIN') || roles.includes('ROLE_ADMIN')) return 'ROLE_ADMIN';
  if (roles.includes('DEFESA_CIVIL') || roles.includes('ROLE_DEFESA_CIVIL')) return 'ROLE_DEFESA_CIVIL';
  if (roles.includes('MUNICIPAL_OPERATOR') || roles.includes('ROLE_OPERATOR') || roles.includes('OPERATOR')) return 'ROLE_OPERATOR';
  return 'ROLE_CITIZEN';
}

const BASE_URL = '/api/v1';

class ApiClient {
  private getAuthHeader(): Record<string, string> {
    const token = localStorage.getItem('guardian_access_token');
    if (!token) return {};
    return { Authorization: `Bearer ${token}` };
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...this.getAuthHeader(),
      ...(options.headers as Record<string, string>),
    };

    const response = await fetch(`${BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    if (response.status === 401) {
      localStorage.removeItem('guardian_access_token');
      localStorage.removeItem('guardian_refresh_token');
      localStorage.removeItem('guardian_user');
      window.dispatchEvent(new Event('guardian_auth_expired'));
    }

    const contentType = response.headers.get('content-type');
    const isJson = contentType && contentType.includes('application/json');
    const responseData = isJson ? await response.json() : null;

    if (!response.ok) {
      const errorMessage =
        responseData?.message ||
        responseData?.error ||
        `Erro ${response.status}: ${response.statusText}`;
      throw new Error(errorMessage);
    }

    return responseData as T;
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
    const rolesList = Array.isArray(rawUser.roles)
      ? rawUser.roles
      : rawUser.role
      ? [rawUser.role]
      : [];
    const mappedRole = mapRole(rolesList);

    const user: UserSummary = {
      id: rawUser.id,
      name: rawUser.name,
      email: rawUser.email || email,
      role: mappedRole,
      municipalityId: rawUser.municipalityId || null,
      permissions: rawUser.permissions || [],
    };

    const tokens: AuthTokens = {
      accessToken,
      refreshToken,
      tokenType,
      expiresIn,
    };

    localStorage.setItem('guardian_access_token', accessToken);
    if (refreshToken) {
      localStorage.setItem('guardian_refresh_token', refreshToken);
    }
    localStorage.setItem('guardian_user', JSON.stringify(user));

    return {
      tokens,
      user,
    };
  }

  logout(): void {
    localStorage.removeItem('guardian_access_token');
    localStorage.removeItem('guardian_refresh_token');
    localStorage.removeItem('guardian_user');
    window.location.reload();
  }

  // --- Municipalities ---
  async listMunicipalities(): Promise<Municipality[]> {
    const res = await this.request<PaginatedResponse<Municipality>>('/municipalities?size=100');
    return res.data;
  }

  // --- Occurrences ---
  async listOccurrences(params: {
    municipalityId?: string;
    status?: string;
    category?: string;
    page?: number;
    size?: number;
  }): Promise<PaginatedResponse<OccurrenceReport>> {
    const query = new URLSearchParams();
    if (params.municipalityId) query.append('municipalityId', params.municipalityId);
    if (params.status) query.append('status', params.status);
    if (params.category) query.append('category', params.category);
    query.append('page', (params.page ?? 0).toString());
    query.append('size', (params.size ?? 20).toString());

    return this.request<PaginatedResponse<OccurrenceReport>>(`/occurrences?${query.toString()}`);
  }

  async updateOccurrenceStatus(
    id: string,
    status: ReportStatus,
    adminNotes?: string
  ): Promise<OccurrenceReport> {
    const res = await this.request<ApiResponse<OccurrenceReport>>(`/admin/occurrences/${id}`, {
      method: 'PATCH',
      body: JSON.stringify({ status, adminNotes }),
    });
    return res.data;
  }

  // --- Risk Assessments ---
  async getLatestRisk(municipalityId: string): Promise<RiskAssessment> {
    const res = await this.request<ApiResponse<RiskAssessment>>(
      `/risk-assessments/latest?municipalityId=${municipalityId}`
    );
    return res.data;
  }

  async listRiskAssessments(
    municipalityId: string,
    page = 0,
    size = 10
  ): Promise<PaginatedResponse<RiskAssessment>> {
    return this.request<PaginatedResponse<RiskAssessment>>(
      `/risk-assessments?municipalityId=${municipalityId}&page=${page}&size=${size}`
    );
  }

  async recalculateRisk(municipalityId: string): Promise<RiskAssessment> {
    const res = await this.request<ApiResponse<RiskAssessment>>(
      '/admin/risk-assessments/recalculate',
      {
        method: 'POST',
        body: JSON.stringify({ municipalityId }),
      }
    );
    return res.data;
  }

  // --- Alerts ---
  async listAlerts(params: {
    municipalityId?: string;
    activeOnly?: boolean;
    page?: number;
    size?: number;
  }): Promise<PaginatedResponse<Alert>> {
    const query = new URLSearchParams();
    if (params.municipalityId) query.append('municipalityId', params.municipalityId);
    if (params.activeOnly !== undefined) query.append('activeOnly', params.activeOnly.toString());
    query.append('page', (params.page ?? 0).toString());
    query.append('size', (params.size ?? 20).toString());

    return this.request<PaginatedResponse<Alert>>(`/alerts?${query.toString()}`);
  }

  async createAlert(data: {
    municipalityId: string;
    title: string;
    description: string;
    category: string;
    severity: string;
    expiresAt: string;
    recommendations?: string[];
  }): Promise<Alert> {
    const res = await this.request<ApiResponse<Alert>>('/admin/alerts', {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return res.data;
  }

  async closeAlert(id: string, reason: string): Promise<Alert> {
    const res = await this.request<ApiResponse<Alert>>(`/admin/alerts/${id}/close`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    });
    return res.data;
  }

  // --- Safe Locations ---
  async listSafeLocations(params: {
    municipalityId?: string;
    availableOnly?: boolean;
    page?: number;
    size?: number;
  }): Promise<PaginatedResponse<SafeLocation>> {
    const query = new URLSearchParams();
    if (params.municipalityId) query.append('municipalityId', params.municipalityId);
    if (params.availableOnly !== undefined) query.append('availableOnly', params.availableOnly.toString());
    query.append('page', (params.page ?? 0).toString());
    query.append('size', (params.size ?? 20).toString());

    return this.request<PaginatedResponse<SafeLocation>>(`/safe-locations?${query.toString()}`);
  }

  async createSafeLocation(data: {
    municipalityId: string;
    name: string;
    type: string;
    operationalStatus: string;
    capacity: number;
    latitude: number;
    longitude: number;
    address?: string;
    contactPhone?: string;
    accessibilityFeatures?: string[];
  }): Promise<SafeLocation> {
    const res = await this.request<ApiResponse<SafeLocation>>('/admin/safe-locations', {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return res.data;
  }

  async updateSafeLocationAvailability(
    id: string,
    data: {
      operationalStatus: SafeLocationOperationalStatus;
      capacity?: number;
      currentOccupancy?: number;
      notes?: string;
    }
  ): Promise<SafeLocation> {
    const res = await this.request<ApiResponse<SafeLocation>>(
      `/admin/safe-locations/${id}/availability`,
      {
        method: 'PATCH',
        body: JSON.stringify(data),
      }
    );
    return res.data;
  }
}

export const api = new ApiClient();
