ALTER TABLE channels
  ADD COLUMN display_name VARCHAR(128) NULL,
  ADD COLUMN credential_ref VARCHAR(256) NULL,
  ADD COLUMN credential_fingerprint VARCHAR(8) NULL,
  ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'active'
    CHECK (status IN ('active', 'suspended', 'archived')),
  ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ADD CONSTRAINT channels_provider_code_not_reserved
    CHECK (provider_code NOT IN ('none', 'mock', 'dummy')),
  ADD CONSTRAINT channels_credential_ref_envonly
    CHECK (credential_ref IS NULL OR credential_ref ~ '^env://[A-Z][A-Z0-9_]{2,127}$');

UPDATE channels
   SET credential_ref = 'env://LEGACY_PLACEHOLDER',
       credential_fingerprint = '00000000'
 WHERE credential_ref IS NULL;

CREATE INDEX channels_merchant_status_idx
  ON channels (merchant_id, status);
CREATE INDEX channels_provider_status_idx
  ON channels (provider_code, status);
