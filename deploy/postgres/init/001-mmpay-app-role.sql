DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'mmpay_app_role') THEN
    CREATE ROLE mmpay_app_role NOLOGIN;
  END IF;
END$$;

GRANT CONNECT ON DATABASE mmpay TO mmpay_app_role;
GRANT USAGE, CREATE ON SCHEMA public TO mmpay_app_role;
GRANT mmpay_app_role TO mmpay;
