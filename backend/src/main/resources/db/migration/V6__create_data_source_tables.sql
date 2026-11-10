-- V6__create_data_source_tables.sql
CREATE TABLE data_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    base_url VARCHAR(500),
    reliability_level VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE external_observations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id UUID NOT NULL REFERENCES data_sources(id),
    municipality_id UUID REFERENCES municipalities(id),
    event_type VARCHAR(50),
    observed_value JSONB NOT NULL,
    location GEOMETRY(POINT, 4326),
    reference_at TIMESTAMPTZ NOT NULL,
    collected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    freshness_status VARCHAR(20) NOT NULL
);

ALTER TABLE risk_areas
    ADD CONSTRAINT fk_risk_areas_source
    FOREIGN KEY (source_id) REFERENCES data_sources(id) ON DELETE SET NULL;
