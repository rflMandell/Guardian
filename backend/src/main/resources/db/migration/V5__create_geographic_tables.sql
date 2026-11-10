-- V5__create_geographic_tables.sql
CREATE TABLE monitored_areas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    municipality_id UUID NOT NULL REFERENCES municipalities(id),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    geometry GEOMETRY(MULTIPOLYGON, 4326) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE risk_areas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    municipality_id UUID NOT NULL REFERENCES municipalities(id),
    monitored_area_id UUID REFERENCES monitored_areas(id),
    event_type VARCHAR(50) NOT NULL,
    geometry GEOMETRY(MULTIPOLYGON, 4326) NOT NULL,
    vulnerability_level VARCHAR(20) NOT NULL,
    source_id UUID,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
