CREATE TABLE IF NOT EXISTS static_offer (
    id uuid primary key,
    process_token text not null,
    issuer_slug varchar(255) not null,
    created_at timestamptz not null
);
