CREATE TABLE webhook_integrations (
  id VARCHAR(64) PRIMARY KEY,
  display_name VARCHAR(128) NOT NULL,
  target_url VARCHAR(2048) NOT NULL,
  secret_ref VARCHAR(256) NOT NULL,
  secret_fingerprint CHAR(8) NOT NULL,
  legacy_header_alias VARCHAR(128),
  legacy_module_required BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(16) NOT NULL DEFAULT 'active'
    CHECK (status IN ('active', 'suspended', 'archived')),
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT target_url_https CHECK (target_url ~ '^https://[A-Za-z0-9.-]+'),
  CONSTRAINT webhook_secret_ref_env CHECK (secret_ref ~ '^env://[A-Z][A-Z0-9_]{2,127}$')
);

CREATE INDEX webhook_integrations_tenant_status_idx
  ON webhook_integrations (tenant_id, status);
