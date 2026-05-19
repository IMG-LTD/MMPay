INSERT INTO sys_role (role) VALUES ('admin'), ('ops'), ('finance'), ('auditor');

INSERT INTO sys_permission (permission) VALUES
  ('admin:read'),
  ('admin:write'),
  ('audit:read'),
  ('audit:verify'),
  ('service-principal:write');

INSERT INTO sys_role_permission (role, permission) VALUES
  ('admin', 'admin:read'),
  ('admin', 'admin:write'),
  ('admin', 'audit:read'),
  ('admin', 'audit:verify'),
  ('admin', 'service-principal:write'),
  ('ops', 'admin:read'),
  ('finance', 'admin:read'),
  ('auditor', 'admin:read'),
  ('auditor', 'audit:read'),
  ('auditor', 'audit:verify');
