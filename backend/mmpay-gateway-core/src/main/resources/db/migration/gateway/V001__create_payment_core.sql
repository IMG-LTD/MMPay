CREATE TABLE merchants (
  id VARCHAR(64) PRIMARY KEY,
  display_name VARCHAR(128) NOT NULL,
  credential_handle VARCHAR(256) NOT NULL
);

CREATE TABLE channels (
  id VARCHAR(64) PRIMARY KEY,
  merchant_id VARCHAR(64) NOT NULL,
  provider_code VARCHAR(64) NOT NULL,
  credential_handle VARCHAR(256) NOT NULL,
  CONSTRAINT fk_channels_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id)
);

CREATE TABLE payment_intents (
  id VARCHAR(64) PRIMARY KEY,
  amount_minor BIGINT NOT NULL,
  currency VARCHAR(8) NOT NULL,
  order_ref VARCHAR(128) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL UNIQUE,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE transactions (
  id VARCHAR(64) PRIMARY KEY,
  payment_intent_id VARCHAR(64) NOT NULL,
  amount_minor BIGINT NOT NULL,
  currency VARCHAR(8) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE refunds (
  id VARCHAR(64) PRIMARY KEY,
  transaction_id VARCHAR(64) NOT NULL,
  amount_minor BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  requested_at TIMESTAMP NOT NULL
);
