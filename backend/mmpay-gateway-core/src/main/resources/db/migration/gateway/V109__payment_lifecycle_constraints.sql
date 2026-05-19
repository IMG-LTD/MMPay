ALTER TABLE refunds
  ADD CONSTRAINT fk_refunds_payment_intent FOREIGN KEY (payment_intent_id) REFERENCES payment_intents (id) ON DELETE RESTRICT;

CREATE INDEX payment_intents_channel_created_idx
  ON payment_intents (channel_id, created_at DESC);

CREATE INDEX payment_intents_merchant_status_idx
  ON payment_intents (merchant_id, status, tenant_id);

CREATE INDEX transactions_payment_intent_idx
  ON transactions (payment_intent_id);

CREATE INDEX refunds_payment_intent_status_idx
  ON refunds (payment_intent_id, status);

CREATE OR REPLACE FUNCTION payment_intent_active_reference_guard()
RETURNS TRIGGER AS $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM merchants WHERE id = NEW.merchant_id AND status <> 'archived') THEN
    RAISE EXCEPTION 'payment intent merchant reference is not active'
      USING ERRCODE = '23503';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM channels WHERE id = NEW.channel_id AND status <> 'archived') THEN
    RAISE EXCEPTION 'payment intent channel reference is not active'
      USING ERRCODE = '23503';
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER payment_intents_active_reference_guard
BEFORE INSERT OR UPDATE OF merchant_id, channel_id ON payment_intents
FOR EACH ROW
EXECUTE FUNCTION payment_intent_active_reference_guard();

CREATE OR REPLACE FUNCTION refund_total_guard()
RETURNS TRIGGER AS $$
DECLARE
  intent_amount BIGINT;
  intent_currency VARCHAR(8);
  refunded_total BIGINT;
BEGIN
  SELECT amount_minor, currency INTO intent_amount, intent_currency
    FROM payment_intents
   WHERE id = NEW.payment_intent_id
   FOR UPDATE;

  IF intent_currency <> NEW.currency THEN
    RAISE EXCEPTION 'refund currency mismatch'
      USING ERRCODE = '23514';
  END IF;

  SELECT COALESCE(SUM(amount_minor), 0) INTO refunded_total
    FROM refunds
   WHERE payment_intent_id = NEW.payment_intent_id
     AND id <> NEW.id
     AND status <> 'cancelled';

  IF refunded_total + NEW.amount_minor > intent_amount THEN
    RAISE EXCEPTION 'refund total exceeds payment intent'
      USING ERRCODE = '23514';
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER refunds_refund_total_guard
BEFORE INSERT OR UPDATE ON refunds
FOR EACH ROW
EXECUTE FUNCTION refund_total_guard();

CREATE OR REPLACE FUNCTION payment_intent_terminal_guard()
RETURNS TRIGGER AS $$
BEGIN
  IF OLD.status IN ('failed', 'cancelled', 'refunded') AND NEW.status <> OLD.status THEN
    RAISE EXCEPTION 'payment intent terminal state cannot change'
      USING ERRCODE = '23514';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER payment_intents_terminal_guard
BEFORE UPDATE ON payment_intents
FOR EACH ROW
EXECUTE FUNCTION payment_intent_terminal_guard();
