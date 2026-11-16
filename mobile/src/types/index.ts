export type EventCategory =
  | 'FLOODING'
  | 'FLOOD'
  | 'LANDSLIDE'
  | 'FLASH_FLOOD'
  | 'HEAVY_RAIN'
  | 'STORM_STRONG_WINDS'
  | 'CONTAINMENT_FAILURE';

export type ReportStatus =
  | 'RECEIVED'
  | 'UNDER_ANALYSIS'
  | 'LINKED_TO_INCIDENT'
  | 'DISMISSED'
  | 'CLOSED';

export type RiskLevel = 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';
export type ConfidenceLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type AssessmentStatus = 'VALID' | 'INSUFFICIENT_DATA' | 'STALE_DATA';

export interface OccurrenceReport {
  id: string;
  municipalityId: string;
  userId?: string | null;
  category: EventCategory;
  status: ReportStatus;
  description: string;
  latitude: number;
  longitude: number;
  addressReference?: string;
  occurredAt: string;
  createdAt: string;
}

export interface RiskFactor {
  factorName: string;
  weight: number;
  contributionLevel: string;
  description: string;
  observedValue?: Record<string, unknown> | null;
}

export interface RiskAssessment {
  id: string;
  municipalityId: string;
  overallRiskLevel: RiskLevel;
  confidenceLevel: ConfidenceLevel;
  status: AssessmentStatus;
  summary: string;
  evaluatedAt: string;
  validUntil: string;
  factors: RiskFactor[];
}

export type AlertSeverity = 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';
export type AlertOriginType = 'GUARDIAN' | 'OFFICIAL' | 'INTEGRATION';
export type AlertStatus = 'ACTIVE' | 'SUPERSEDED' | 'EXPIRED' | 'CANCELLED' | 'CLOSED';

export interface Alert {
  id: string;
  municipalityId: string;
  title: string;
  description: string;
  category: EventCategory;
  severity: AlertSeverity;
  origin: AlertOriginType;
  status: AlertStatus;
  issuedAt: string;
  expiresAt: string;
  recommendations: string[];
}

export type SafeLocationType =
  | 'TEMPORARY_SHELTER'
  | 'COMMUNITY_CENTER'
  | 'PUBLIC_SCHOOL'
  | 'SPORTS_ARENA'
  | 'HEALTH_UNIT';

export type SafeLocationAvailability =
  | 'AVAILABLE'
  | 'LIMITED'
  | 'FULL'
  | 'UNAVAILABLE';

export interface SafeLocation {
  id: string;
  municipalityId: string;
  name: string;
  type: SafeLocationType;
  operationalStatus: string;
  availability: SafeLocationAvailability;
  capacity?: number | null;
  currentOccupancy?: number | null;
  latitude: number;
  longitude: number;
  address?: string | null;
  contactPhone?: string | null;
  accessibilityFeatures?: string[] | null;
}

export interface UserSummary {
  id: string;
  name: string;
  email: string;
  role: string;
  municipalityId?: string | null;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface LoginResponseData {
  tokens: AuthTokens;
  user: UserSummary;
}

export interface Municipality {
  id: string;
  name: string;
  state: string;
  country: string;
}

export interface GeolocationStatus {
  withinCoverage: boolean;
  municipalityId?: string;
  municipalityName?: string;
  inRiskArea: boolean;
  activeRiskLevel?: RiskLevel;
  activeAlertsCount: number;
}
