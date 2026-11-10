-- V4__create_municipality_tables.sql
CREATE TABLE municipalities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    state CHAR(2) NOT NULL,
    ibge_code VARCHAR(10) NOT NULL,
    country_code CHAR(2) NOT NULL DEFAULT 'BR',
    timezone VARCHAR(60) NOT NULL DEFAULT 'America/Sao_Paulo',
    boundary GEOMETRY(MULTIPOLYGON, 4326),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_municipality_ibge UNIQUE (ibge_code)
);

CREATE TABLE municipality_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    municipality_id UUID NOT NULL REFERENCES municipalities(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_user_municipality UNIQUE (user_id, municipality_id),
    CONSTRAINT ck_municipality_membership_status CHECK (status IN ('ACTIVE', 'PENDING', 'REVOKED'))
);

ALTER TABLE citizen_profiles
    ADD CONSTRAINT fk_citizen_profiles_municipality
    FOREIGN KEY (municipality_id) REFERENCES municipalities(id) ON DELETE SET NULL;
