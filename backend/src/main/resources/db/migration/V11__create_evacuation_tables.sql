-- V11__create_evacuation_tables.sql
CREATE TABLE evacuation_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    municipality_id UUID NOT NULL REFERENCES municipalities(id),
    safe_location_id UUID REFERENCES safe_locations(id),
    origin_location GEOMETRY(POINT, 4326) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE TABLE evacuation_routes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES evacuation_sessions(id) ON DELETE CASCADE,
    route_geometry GEOMETRY(LINESTRING, 4326) NOT NULL,
    distance_meters NUMERIC(10,2),
    estimated_duration_seconds INTEGER,
    instructions JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
