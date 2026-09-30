CREATE TABLE IF NOT EXISTS tg4u_auth_flow (
    uuid UUID PRIMARY KEY,
    credential_identifier TEXT NOT NULL,
    credential_version TEXT NOT NULL,
    dcql_query TEXT,
    credential_mapping TEXT,
    display_name TEXT,
    client_id TEXT NOT NULL,
    client_secret_ciphertext TEXT NOT NULL,
    authorize_endpoint TEXT NOT NULL,
    token_endpoint TEXT NOT NULL,
    scope TEXT,
    jwks_endpoint TEXT NOT NULL,
    provider_issuer TEXT NOT NULL,
    audience TEXT
);

CREATE TABLE IF NOT EXISTS tg4u_authorization_session (
    id UUID PRIMARY KEY,
    auth_flow_id UUID NOT NULL REFERENCES tg4u_auth_flow(uuid),
    issuer_slug TEXT NOT NULL,
    variant TEXT NOT NULL,
    credential_identifier TEXT NOT NULL,
    credential_version TEXT NOT NULL,
    issuer_state TEXT NOT NULL UNIQUE,
    external_state TEXT NOT NULL UNIQUE,
    connection_id TEXT NOT NULL UNIQUE,
    redirect_uri TEXT,
    client_state TEXT,
    code_challenge TEXT,
    requested_dpop_jkt TEXT,
    authorization_code TEXT UNIQUE,
    pre_authorized_code TEXT,
    issuer_connection_id TEXT,
    status TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS tg4u_pushed_authorization_request (
    request_uri TEXT PRIMARY KEY,
    parameters TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
