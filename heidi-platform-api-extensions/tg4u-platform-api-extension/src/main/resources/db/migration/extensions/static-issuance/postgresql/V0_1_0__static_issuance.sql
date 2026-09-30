CREATE TABLE IF NOT EXISTS t_static_flow
(
    pk_static_flow       INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    uuid                 UUID      NOT NULL UNIQUE,
    fk_credential_scheme INTEGER   NOT NULL,
    attributes           JSONB,
    issuer_slug          TEXT      NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE,
    tx_code              TEXT,
    display_name         TEXT,
    FOREIGN KEY (fk_credential_scheme) REFERENCES t_credential_scheme (pk_credential_scheme_id)
);

CREATE TABLE IF NOT EXISTS t_static_qr_code_flow
(
    pk_static_qr_code_flow INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    uuid                   UUID      NOT NULL UNIQUE,
    action_payload         JSONB     NOT NULL,
    display_name           TEXT,
    tenant_id              TEXT      NOT NULL,
    is_public              BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP WITH TIME ZONE
);
