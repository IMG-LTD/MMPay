ALTER TABLE payment_intents
  ADD COLUMN IF NOT EXISTS qr_code_url VARCHAR(2048);
