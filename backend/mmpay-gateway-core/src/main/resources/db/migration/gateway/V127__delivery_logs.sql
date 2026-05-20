CREATE TABLE delivery_logs (
  id BIGSERIAL PRIMARY KEY,
  integration_id VARCHAR(64) NOT NULL,
  payment_intent_id VARCHAR(64) NOT NULL,
  event_id UUID NOT NULL,
  attempt INT NOT NULL,
  scheduled_at TIMESTAMPTZ NOT NULL,
  dispatched_at TIMESTAMPTZ,
  response_status INT,
  response_body_sha256 CHAR(64),
  next_retry_at TIMESTAMPTZ,
  dead_letter BOOLEAN NOT NULL DEFAULT FALSE,
  bulk_redispatch_batch_id UUID,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default'
);

CREATE INDEX delivery_logs_integration_dead_letter_idx
  ON delivery_logs (integration_id, dead_letter, scheduled_at);

CREATE INDEX delivery_logs_payment_event_idx
  ON delivery_logs (payment_intent_id, event_id);

CREATE INDEX delivery_logs_next_retry_idx
  ON delivery_logs (next_retry_at)
  WHERE next_retry_at IS NOT NULL;
