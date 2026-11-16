-- V15__alter_municipality_char_to_varchar.sql
ALTER TABLE municipalities
    ALTER COLUMN country_code TYPE VARCHAR(2),
    ALTER COLUMN state TYPE VARCHAR(2);
