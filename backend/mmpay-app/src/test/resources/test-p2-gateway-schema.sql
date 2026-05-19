DROP TABLE IF EXISTS channels;
DROP TABLE IF EXISTS merchants;

CREATE TABLE merchants (
  row_uid VARCHAR(36) PRIMARY KEY,
  id VARCHAR(64) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  credential_handle VARCHAR(256) NOT NULL,
  credential_ref VARCHAR(256),
  credential_fingerprint VARCHAR(8),
  status VARCHAR(16) NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX merchants_id_active_uniq
  ON merchants (id, status);

CREATE TABLE channels (
  row_uid VARCHAR(36) PRIMARY KEY,
  id VARCHAR(64) NOT NULL,
  merchant_id VARCHAR(64) NOT NULL,
  display_name VARCHAR(128),
  provider_code VARCHAR(64) NOT NULL,
  credential_handle VARCHAR(256) NOT NULL,
  credential_ref VARCHAR(256),
  credential_fingerprint VARCHAR(8),
  status VARCHAR(16) NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX channels_id_active_uniq
  ON channels (id, status);
