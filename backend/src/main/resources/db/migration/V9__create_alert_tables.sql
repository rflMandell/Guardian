-- V9__create_alert_tables.sql
CREATE TABLE alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    municipality_id UUID NOT NULL REFERENCES municipalities(id),
    incident_id UUID REFERENCES incidents(id),
    risk_assessment_id UUID REFERENCES risk_assessments(id),
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL,
    origin_type VARCHAR(30) NOT NULL,
    source_id UUID REFERENCES data_sources(id),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_by UUID REFERENCES users(id),
    issued_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE alert_areas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_id UUID NOT NULL REFERENCES alerts(id) ON DELETE CASCADE,
    geometry GEOMETRY(MULTIPOLYGON, 4326) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE alert_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_id UUID NOT NULL REFERENCES alerts(id) ON DELETE CASCADE,
    from_status VARCHAR(20) NOT NULL,
    to_status VARCHAR(20) NOT NULL,
    changed_by UUID REFERENCES users(id),
    reason TEXT,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
