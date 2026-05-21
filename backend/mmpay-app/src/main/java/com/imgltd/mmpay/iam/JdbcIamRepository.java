package com.imgltd.mmpay.iam;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcIamRepository {
  private static final String CLIENT_AUTH_BASIC = "client_secret_basic";
  private static final String CLIENT_SETTINGS = "{\"settings.client.require-proof-key\":false}";
  private static final String TOKEN_SETTINGS = "{}";
  private final JdbcTemplate jdbcTemplate;

  public JdbcIamRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void createServicePrincipal(ServicePrincipalRecord principal) {
    jdbcTemplate.update(
        "INSERT INTO sys_user (id, username, kind, password_hash, role, secret_fingerprint) "
            + "VALUES (?, ?, 'service', NULL, ?, ?)",
        principal.userId(),
        principal.clientId(),
        principal.role(),
        principal.secretFingerprint());
    jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role) VALUES (?, ?)", principal.userId(), principal.role());
    jdbcTemplate.update(
        "INSERT INTO oauth2_registered_client (id, client_id, client_secret, client_name, "
            + "client_authentication_methods, authorization_grant_types, scopes, client_settings, token_settings) "
            + "VALUES (?, ?, ?, ?, ?, 'client_credentials', ?, ?, ?)",
        principal.registeredClientId(),
        principal.clientId(),
        principal.secretFingerprint(),
        principal.clientId(),
        CLIENT_AUTH_BASIC,
        "role:" + principal.role(),
        CLIENT_SETTINGS,
        TOKEN_SETTINGS);
  }

  public Optional<RegisteredClientRecord> findRegisteredClient(String clientId) {
    try {
      return Optional.ofNullable(
          jdbcTemplate.queryForObject(
              "SELECT * FROM oauth2_registered_client WHERE client_id = ?", this::registeredClient, clientId));
    } catch (EmptyResultDataAccessException exception) {
      return Optional.empty();
    }
  }

  public void saveAuthorization(TokenRecord token) {
    jdbcTemplate.update(
        "INSERT INTO oauth2_authorization (id, registered_client_id, principal_name, authorization_grant_type, "
            + "authorized_scopes, access_token_value, access_token_issued_at, access_token_expires_at, "
            + "refresh_token_value, refresh_token_issued_at, refresh_token_expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        token.id(),
        token.registeredClientId(),
        token.principalName(),
        token.grantType(),
        String.join(",", token.scopes()),
        token.accessToken(),
        Timestamp.from(token.issuedAt()),
        Timestamp.from(token.expiresAt()),
        token.refreshToken(),
        timestamp(token.refreshTokenIssuedAt()),
        timestamp(token.refreshTokenExpiresAt()));
  }

  public Optional<TokenPrincipalRecord> findAccessToken(String accessToken, Instant now) {
    try {
      return Optional.ofNullable(
          jdbcTemplate.queryForObject(
              "SELECT a.principal_name, a.access_token_expires_at, u.role FROM oauth2_authorization a "
                  + "JOIN sys_user u ON u.username = a.principal_name WHERE a.access_token_value = ? "
                  + "AND a.access_token_expires_at > ?",
              this::tokenPrincipal,
              accessToken,
              Timestamp.from(now)));
    } catch (EmptyResultDataAccessException exception) {
      return Optional.empty();
    }
  }

  public Optional<IamUserRecord> findUser(String username) {
    try {
      return Optional.ofNullable(
          jdbcTemplate.queryForObject("SELECT * FROM sys_user WHERE username = ?", this::user, username));
    } catch (EmptyResultDataAccessException exception) {
      return Optional.empty();
    }
  }

  public Optional<RefreshTokenRecord> findRefreshToken(String refreshToken, Instant now) {
    try {
      return Optional.ofNullable(
          jdbcTemplate.queryForObject(
              "SELECT a.registered_client_id, a.principal_name, a.refresh_token_value, u.role "
                  + "FROM oauth2_authorization a JOIN sys_user u ON u.username = a.principal_name "
                  + "WHERE a.refresh_token_value = ? AND a.refresh_token_expires_at > ?",
              this::refreshToken,
              refreshToken,
              Timestamp.from(now)));
    } catch (EmptyResultDataAccessException exception) {
      return Optional.empty();
    }
  }

  public void consumeRefreshToken(String refreshToken) {
    jdbcTemplate.update(
        "UPDATE oauth2_authorization SET refresh_token_value = NULL WHERE refresh_token_value = ?", refreshToken);
  }

  public List<IamUserRecord> listHumanUsers() {
    return jdbcTemplate.query(
        "SELECT * FROM sys_user WHERE kind = 'user' ORDER BY created_at ASC", this::user);
  }

  public void createHumanUser(String id, String username, String passwordHash, String role) {
    jdbcTemplate.update(
        "INSERT INTO sys_user (id, username, kind, password_hash, role) VALUES (?, ?, 'user', ?, ?)",
        id, username, passwordHash, role);
    jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role) VALUES (?, ?)", id, role);
  }

  public boolean updateUserRole(String username, String role) {
    int rows = jdbcTemplate.update(
        "UPDATE sys_user SET role = ? WHERE username = ? AND kind = 'user'", role, username);
    if (rows > 0) {
      jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = (SELECT id FROM sys_user WHERE username = ?)", username);
      jdbcTemplate.update(
          "INSERT INTO sys_user_role (user_id, role) SELECT id, ? FROM sys_user WHERE username = ?", role, username);
    }
    return rows > 0;
  }

  public boolean updateUserPassword(String username, String passwordHash) {
    int rows = jdbcTemplate.update(
        "UPDATE sys_user SET password_hash = ? WHERE username = ? AND kind = 'user'", passwordHash, username);
    return rows > 0;
  }

  public boolean deleteHumanUser(String username) {
    jdbcTemplate.update(
        "DELETE FROM sys_user_role WHERE user_id = (SELECT id FROM sys_user WHERE username = ? AND kind = 'user')", username);
    jdbcTemplate.update(
        "DELETE FROM oauth2_authorization WHERE principal_name = ?", username);
    int rows = jdbcTemplate.update(
        "DELETE FROM sys_user WHERE username = ? AND kind = 'user'", username);
    return rows > 0;
  }

  private RegisteredClientRecord registeredClient(ResultSet resultSet, int rowNumber) throws SQLException {
    return new RegisteredClientRecord(
        resultSet.getString("id"),
        resultSet.getString("client_id"),
        resultSet.getString("client_secret"),
        csv(resultSet.getString("authorization_grant_types")),
        csv(resultSet.getString("scopes")));
  }

  private TokenPrincipalRecord tokenPrincipal(ResultSet resultSet, int rowNumber) throws SQLException {
    return new TokenPrincipalRecord(
        resultSet.getString("principal_name"),
        resultSet.getString("role"),
        resultSet.getTimestamp("access_token_expires_at").toInstant());
  }

  private IamUserRecord user(ResultSet resultSet, int rowNumber) throws SQLException {
    var ts = resultSet.getTimestamp("created_at");
    return new IamUserRecord(
        resultSet.getString("username"),
        resultSet.getString("kind"),
        resultSet.getString("password_hash"),
        resultSet.getString("role"),
        ts != null ? ts.toInstant().toString() : null);
  }

  private RefreshTokenRecord refreshToken(ResultSet resultSet, int rowNumber) throws SQLException {
    return new RefreshTokenRecord(
        resultSet.getString("registered_client_id"),
        resultSet.getString("principal_name"),
        resultSet.getString("refresh_token_value"),
        resultSet.getString("role"));
  }

  private Timestamp timestamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }

  private Set<String> csv(String value) {
    return Arrays.stream(value.split(",")).map(String::trim).filter(token -> !token.isBlank()).collect(Collectors.toSet());
  }
}
