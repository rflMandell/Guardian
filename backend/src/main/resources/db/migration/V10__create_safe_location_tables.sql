-- V10__create_safe_location_tables.sql
CREATE TABLE safe_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    municipality_id UUID NOT NULL REFERENCES municipalities(id),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    location GEOMETRY(POINT, 4326) NOT NULL,
    address VARCHAR(255),
    location_type VARCHAR(30) NOT NULL,
    maximum_capacity INTEGER,
    operational_status VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    accessibility_info TEXT,
    operating_hours JSONB,
    source_id UUID REFERENCES data_sources(id),
    last_verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_safe_location_capacity
        CHECK (maximum_capacity IS NULL OR maximum_capacity >= 0)
);

CREATE TABLE safe_location_occupancies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    safe_location_id UUID NOT NULL REFERENCES safe_locations(id) ON DELETE CASCADE,
    occupancy_count INTEGER NOT NULL,
    source_id UUID REFERENCES data_sources(id),
    reported_at TIMESTAMPTZ NOT NULL,
    collected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_occupancy_count CHECK (occupancy_count >= 0)
);
