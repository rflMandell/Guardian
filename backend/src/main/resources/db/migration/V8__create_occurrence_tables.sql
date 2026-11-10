-- V8__create_occurrence_tables.sql
CREATE TABLE incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    municipality_id UUID NOT NULL REFERENCES municipalities(id),
    event_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    location GEOMETRY(POINT, 4326) NOT NULL,
    status VARCHAR(30) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    origin VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMPTZ
);

CREATE TABLE occurrence_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    municipality_id UUID NOT NULL REFERENCES municipalities(id),
    event_type VARCHAR(50) NOT NULL,
    description TEXT,
    location GEOMETRY(POINT, 4326) NOT NULL,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'UNVERIFIED',
    reliability_score NUMERIC(5,2),
    reported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE incident_report_links (
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    report_id UUID NOT NULL REFERENCES occurrence_reports(id) ON DELETE CASCADE,
    linking_method VARCHAR(20) NOT NULL,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (incident_id, report_id),
    CONSTRAINT uk_report_active_link UNIQUE (report_id)
);
