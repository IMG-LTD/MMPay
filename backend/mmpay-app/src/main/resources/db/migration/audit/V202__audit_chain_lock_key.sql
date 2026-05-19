CREATE TABLE audit_constants (
  name VARCHAR(64) PRIMARY KEY,
  value BIGINT NOT NULL
);

INSERT INTO audit_constants (name, value)
VALUES ('audit_event_chain_lock_id', 7341);
