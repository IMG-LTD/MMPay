ALTER TABLE merchants
  ADD COLUMN credential_ref VARCHAR(256) NULL,
  ADD COLUMN credential_fingerprint VARCHAR(8) NULL,
  ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'active'
    CHECK (status IN ('active', 'suspended', 'archived')),
  ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ADD CONSTRAINT credential_ref_envonly
    CHECK (credential_ref IS NULL OR credential_ref ~ '^env://[A-Z][A-Z0-9_]{2,127}$');

UPDATE merchants
   SET credential_ref = 'env://LEGACY_PLACEHOLDER',
       credential_fingerprint = '00000000'
 WHERE credential_ref IS NULL;

CREATE INDEX merchants_tenant_status_idx
  ON merchants (tenant_id, status);
