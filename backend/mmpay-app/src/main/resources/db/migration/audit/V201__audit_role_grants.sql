DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'mmpay_app_role') THEN
    CREATE ROLE mmpay_app_role NOLOGIN;
  END IF;
END$$;

GRANT USAGE ON SEQUENCE audit_event_id_seq TO mmpay_app_role;
GRANT SELECT, INSERT ON audit_event TO mmpay_app_role;
REVOKE UPDATE, DELETE, TRUNCATE ON audit_event FROM mmpay_app_role;

DO $$
DECLARE
  runtime_user TEXT := COALESCE(current_setting('mmpay.runtime_user', TRUE), 'mmpay');
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = runtime_user) THEN
    EXECUTE format('GRANT mmpay_app_role TO %I', runtime_user);
  END IF;
END$$;
