CREATE TABLE provider_events (
  id BIGSERIAL PRIMARY KEY,
  provider_code VARCHAR(32) NOT NULL,
  provider_event_id VARCHAR(128) NOT NULL,
  payment_intent_id VARCHAR(64),
  raw_signature_sha256 CHAR(64) NOT NULL,
  accepted_at TIMESTAMPTZ NOT NULL,
  duplicate_count INT NOT NULL DEFAULT 0,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  UNIQUE (provider_code, provider_event_id)
);

CREATE INDEX provider_events_payment_intent_idx
  ON provider_events (payment_intent_id);
