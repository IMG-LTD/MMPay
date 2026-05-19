CREATE TABLE IF NOT EXISTS license_relay_logs (
  id BIGSERIAL PRIMARY KEY,
  target_id VARCHAR(64) NOT NULL,
  request_id VARCHAR(64) NOT NULL,
  attempt SMALLINT NOT NULL CHECK (attempt > 0),
  byte_count INTEGER NOT NULL CHECK (byte_count >= 0),
  payload_sha256 CHAR(64) NOT NULL,
  http_status SMALLINT,
  response_sha256 CHAR(64),
  response_size_bytes INTEGER,
  response_truncated BOOLEAN NOT NULL DEFAULT false,
  error_class VARCHAR(64),
  synthetic BOOLEAN NOT NULL DEFAULT false,
  vendor_cert_issuer_dn_sha256 CHAR(64),
  vendor_cert_serial VARCHAR(64),
  dead_letter BOOLEAN NOT NULL DEFAULT false,
  dispatched_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  CONSTRAINT fk_license_relay_logs_target
    FOREIGN KEY (target_id) REFERENCES license_relay_targets (id)
);

CREATE INDEX IF NOT EXISTS ix_license_relay_logs_target_dispatched
  ON license_relay_logs (target_id, dispatched_at DESC);
