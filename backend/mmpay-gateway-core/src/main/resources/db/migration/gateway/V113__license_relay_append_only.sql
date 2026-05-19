DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'mmpay_app_role') THEN
    REVOKE UPDATE, DELETE, TRUNCATE ON license_relay_logs FROM mmpay_app_role;
    GRANT INSERT, SELECT ON license_relay_logs TO mmpay_app_role;
  END IF;
END $$;

CREATE OR REPLACE FUNCTION prevent_license_relay_log_mutation()
RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'license_relay_logs are append-only';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_license_relay_logs_append_only_update ON license_relay_logs;
CREATE TRIGGER trg_license_relay_logs_append_only_update
  BEFORE UPDATE OR DELETE ON license_relay_logs
  FOR EACH ROW EXECUTE FUNCTION prevent_license_relay_log_mutation();
