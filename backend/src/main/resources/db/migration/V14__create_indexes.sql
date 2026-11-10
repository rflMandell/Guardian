-- V14__create_indexes.sql

-- Índices Espaciais (GiST)
CREATE INDEX idx_municipalities_boundary
ON municipalities USING GIST (boundary);

CREATE INDEX idx_monitored_areas_geometry
ON monitored_areas USING GIST (geometry);

CREATE INDEX idx_risk_areas_geometry
ON risk_areas USING GIST (geometry);

CREATE INDEX idx_incidents_location
ON incidents USING GIST (location);

CREATE INDEX idx_occurrence_reports_location
ON occurrence_reports USING GIST (location);

CREATE INDEX idx_safe_locations_location
ON safe_locations USING GIST (location);

-- Índices Relacionais (B-tree)
CREATE INDEX idx_incidents_municipality_status
ON incidents (municipality_id, status);

CREATE INDEX idx_incidents_created_at
ON incidents (created_at DESC);

CREATE INDEX idx_alerts_municipality_status
ON alerts (municipality_id, status);

CREATE INDEX idx_alerts_expires_at
ON alerts (expires_at);

CREATE INDEX idx_risk_assessments_municipality_date
ON risk_assessments (municipality_id, evaluated_at DESC);

CREATE INDEX idx_occurrence_reports_municipality_date
ON occurrence_reports (municipality_id, reported_at DESC);

CREATE INDEX idx_safe_locations_municipality_status
ON safe_locations (municipality_id, operational_status);

CREATE INDEX idx_users_email
ON users (email);
