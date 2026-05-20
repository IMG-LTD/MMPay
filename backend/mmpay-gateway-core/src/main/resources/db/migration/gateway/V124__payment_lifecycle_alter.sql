INSERT INTO merchants (id, display_name, credential_handle, credential_ref, credential_fingerprint, status, tenant_id)
SELECT 'legacy_merchant', 'Legacy migration merchant', 'env://LEGACY_PLACEHOLDER', 'env://LEGACY_PLACEHOLDER', '00000000', 'active', 'default'
WHERE NOT EXISTS (SELECT 1 FROM merchants WHERE id = 'legacy_merchant' AND status <> 'archived');

INSERT INTO channels (id, merchant_id, provider_code, credential_handle, display_name, credential_ref, credential_fingerprint, status, tenant_id)
SELECT 'legacy_channel', 'legacy_merchant', 'huifu', 'env://LEGACY_PLACEHOLDER', 'Legacy migration channel',
       'env://LEGACY_PLACEHOLDER', '00000000', 'active', 'default'
WHERE NOT EXISTS (SELECT 1 FROM channels WHERE id = 'legacy_channel' AND status <> 'archived');

ALTER TABLE payment_intents
  ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64),
  ADD COLUMN IF NOT EXISTS channel_id VARCHAR(64),
  ADD COLUMN IF NOT EXISTS provider_order_id VARCHAR(128),
  ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

UPDATE payment_intents
   SET merchant_id = COALESCE(merchant_id, 'legacy_merchant'),
       channel_id = COALESCE(channel_id, 'legacy_channel'),
       updated_at = COALESCE(updated_at, created_at AT TIME ZONE 'UTC')
 WHERE merchant_id IS NULL OR channel_id IS NULL OR updated_at IS NULL;

UPDATE payment_intents
   SET status = CASE status
     WHEN 'REQUIRES_PAYMENT' THEN 'pending'
     WHEN 'PROCESSING' THEN 'submitted'
     WHEN 'SUCCEEDED' THEN 'succeeded'
     WHEN 'FAILED' THEN 'failed'
     ELSE lower(status)
   END;

ALTER TABLE payment_intents
  ALTER COLUMN merchant_id SET NOT NULL,
  ALTER COLUMN channel_id SET NOT NULL,
  ALTER COLUMN updated_at SET NOT NULL,
  ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE transactions
  ADD COLUMN IF NOT EXISTS provider_event_id VARCHAR(128),
  ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64) NOT NULL DEFAULT 'default';

ALTER TABLE transactions
  ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE refunds
  ADD COLUMN IF NOT EXISTS payment_intent_id VARCHAR(64),
  ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64),
  ADD COLUMN IF NOT EXISTS currency VARCHAR(8),
  ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

UPDATE refunds
   SET payment_intent_id = COALESCE(payment_intent_id, 'legacy_payment_intent'),
       merchant_id = COALESCE(merchant_id, 'legacy_merchant'),
       currency = COALESCE(currency, 'CNY');

INSERT INTO payment_intents (
  id, merchant_id, channel_id, provider_order_id, amount_minor, currency,
  order_ref, idempotency_key, status, tenant_id, created_at, updated_at, version
)
SELECT 'legacy_payment_intent', 'legacy_merchant', 'legacy_channel', NULL, 1, 'CNY',
       'legacy-refund-backfill', 'legacy-refund-backfill', 'succeeded', 'default', now(), now(), 0
WHERE EXISTS (SELECT 1 FROM refunds WHERE payment_intent_id = 'legacy_payment_intent')
  AND NOT EXISTS (SELECT 1 FROM payment_intents WHERE id = 'legacy_payment_intent');

ALTER TABLE refunds
  ALTER COLUMN transaction_id DROP NOT NULL,
  ALTER COLUMN payment_intent_id SET NOT NULL,
  ALTER COLUMN merchant_id SET NOT NULL,
  ALTER COLUMN currency SET NOT NULL,
  ALTER COLUMN requested_at TYPE TIMESTAMPTZ USING requested_at AT TIME ZONE 'UTC';

ALTER TABLE payment_intents
  ADD CONSTRAINT payment_intents_status_check
  CHECK (status IN ('pending', 'submitted', 'succeeded', 'failed', 'cancelled', 'refunded', 'partially_refunded'));
