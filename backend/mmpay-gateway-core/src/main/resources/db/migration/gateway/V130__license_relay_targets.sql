CREATE TABLE IF NOT EXISTS license_relay_targets (
  id VARCHAR(64) PRIMARY KEY,
  display_name VARCHAR(128) NOT NULL,
  target_url VARCHAR(512) NOT NULL,
  status VARCHAR(16) NOT NULL CHECK (status IN ('active','suspended','archived')),
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_license_relay_targets_tenant_id
  ON license_relay_targets (tenant_id, id);
