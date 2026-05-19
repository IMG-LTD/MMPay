CREATE TABLE reconciliation_runs (
  id BIGSERIAL PRIMARY KEY,
  run_date DATE NOT NULL,
  provider_code VARCHAR(32) NOT NULL,
  channel_id VARCHAR(64) NOT NULL,
  ingest_count BIGINT NOT NULL,
  matched_count BIGINT NOT NULL,
  unmatched_count BIGINT NOT NULL,
  outcome VARCHAR(16) NOT NULL CHECK (outcome IN ('ok', 'silent', 'failed')),
  ack_status VARCHAR(16) NOT NULL DEFAULT 'pending'
    CHECK (ack_status IN ('pending', 'acked', 'disputed')),
  ack_at TIMESTAMPTZ,
  ack_actor VARCHAR(64),
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  UNIQUE (run_date, provider_code, channel_id, tenant_id)
);

CREATE INDEX reconciliation_runs_tenant_date_idx
  ON reconciliation_runs (tenant_id, run_date DESC);
