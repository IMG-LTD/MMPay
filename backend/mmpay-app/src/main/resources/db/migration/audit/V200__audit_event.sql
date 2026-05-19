CREATE SEQUENCE audit_event_id_seq;

CREATE TABLE audit_event (
  id BIGINT PRIMARY KEY,
  ts TIMESTAMPTZ NOT NULL,
  actor_kind VARCHAR(16) NOT NULL CHECK (actor_kind IN ('user','service','system')),
  actor_id VARCHAR(64),
  action VARCHAR(64) NOT NULL,
  target_kind VARCHAR(32) NOT NULL,
  target_id VARCHAR(128),
  request_id VARCHAR(64),
  before_hash CHAR(64),
  after_hash CHAR(64),
  details_json JSONB,
  prev_row_hmac CHAR(64),
  row_hmac CHAR(64) NOT NULL,
  tenant_id VARCHAR(32) DEFAULT 'default',
  chain_anchor BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_audit_ts ON audit_event (ts);
CREATE INDEX idx_audit_actor ON audit_event (actor_kind, actor_id);
CREATE INDEX idx_audit_action ON audit_event (action);
CREATE INDEX idx_audit_anchor ON audit_event (chain_anchor) WHERE chain_anchor = TRUE;
