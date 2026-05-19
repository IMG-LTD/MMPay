CREATE TABLE oauth2_registered_client (
  id VARCHAR(100) NOT NULL,
  client_id VARCHAR(100) NOT NULL UNIQUE,
  client_id_issued_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  client_secret VARCHAR(200),
  client_secret_expires_at TIMESTAMPTZ,
  client_name VARCHAR(200) NOT NULL,
  client_authentication_methods VARCHAR(1000) NOT NULL,
  authorization_grant_types VARCHAR(1000) NOT NULL,
  redirect_uris VARCHAR(1000),
  post_logout_redirect_uris VARCHAR(1000),
  scopes VARCHAR(1000) NOT NULL,
  client_settings VARCHAR(2000) NOT NULL,
  token_settings VARCHAR(2000) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE oauth2_authorization (
  id VARCHAR(100) PRIMARY KEY,
  registered_client_id VARCHAR(100) NOT NULL,
  principal_name VARCHAR(200) NOT NULL,
  authorization_grant_type VARCHAR(100) NOT NULL,
  authorized_scopes VARCHAR(1000),
  attributes TEXT,
  state VARCHAR(500),
  authorization_code_value TEXT,
  authorization_code_issued_at TIMESTAMPTZ,
  authorization_code_expires_at TIMESTAMPTZ,
  authorization_code_metadata TEXT,
  access_token_value TEXT,
  access_token_issued_at TIMESTAMPTZ,
  access_token_expires_at TIMESTAMPTZ,
  access_token_metadata TEXT,
  access_token_type VARCHAR(100),
  access_token_scopes VARCHAR(1000),
  oidc_id_token_value TEXT,
  oidc_id_token_issued_at TIMESTAMPTZ,
  oidc_id_token_expires_at TIMESTAMPTZ,
  oidc_id_token_metadata TEXT,
  refresh_token_value TEXT,
  refresh_token_issued_at TIMESTAMPTZ,
  refresh_token_expires_at TIMESTAMPTZ,
  refresh_token_metadata TEXT,
  user_code_value TEXT,
  user_code_issued_at TIMESTAMPTZ,
  user_code_expires_at TIMESTAMPTZ,
  user_code_metadata TEXT,
  device_code_value TEXT,
  device_code_issued_at TIMESTAMPTZ,
  device_code_expires_at TIMESTAMPTZ,
  device_code_metadata TEXT
);

CREATE INDEX idx_oauth2_authorization_access_token ON oauth2_authorization (access_token_value);
CREATE INDEX idx_oauth2_authorization_refresh_token ON oauth2_authorization (refresh_token_value);

CREATE TABLE oauth2_authorization_consent (
  registered_client_id VARCHAR(100) NOT NULL,
  principal_name VARCHAR(200) NOT NULL,
  authorities VARCHAR(1000) NOT NULL,
  PRIMARY KEY (registered_client_id, principal_name)
);

CREATE TABLE sys_user (
  id VARCHAR(64) PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  kind VARCHAR(16) NOT NULL CHECK (kind IN ('user', 'service')),
  password_hash VARCHAR(128),
  role VARCHAR(32) NOT NULL,
  secret_fingerprint VARCHAR(64)
);

CREATE TABLE sys_role (
  role VARCHAR(32) PRIMARY KEY
);

CREATE TABLE sys_permission (
  permission VARCHAR(64) PRIMARY KEY
);

CREATE TABLE sys_user_role (
  user_id VARCHAR(64) NOT NULL,
  role VARCHAR(32) NOT NULL,
  PRIMARY KEY (user_id, role)
);

CREATE TABLE sys_role_permission (
  role VARCHAR(32) NOT NULL,
  permission VARCHAR(64) NOT NULL,
  PRIMARY KEY (role, permission)
);

CREATE UNIQUE INDEX uniq_admin_user ON sys_user (role) WHERE role = 'admin' AND kind = 'user';
