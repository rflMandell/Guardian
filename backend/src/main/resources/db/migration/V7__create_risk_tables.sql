-- V7__create_risk_tables.sql
CREATE TABLE risk_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    municipality_id UUID NOT NULL REFERENCES municipalities(id),
    risk_area_id UUID REFERENCES risk_areas(id),
    event_type VARCHAR(50) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    confidence_level VARCHAR(20) NOT NULL,
    probability_percentage NUMERIC(5,2),
    assessment_status VARCHAR(30) NOT NULL,
    evaluated_at TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ,
    engine_version VARCHAR(30) NOT NULL,

    CONSTRAINT ck_risk_probability
        CHECK (
            probability_percentage IS NULL
            OR probability_percentage BETWEEN 0 AND 100
        )
);

CREATE TABLE risk_factors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    risk_assessment_id UUID NOT NULL REFERENCES risk_assessments(id) ON DELETE CASCADE,
    factor_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    weight NUMERIC(5,2),
    observed_value JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
