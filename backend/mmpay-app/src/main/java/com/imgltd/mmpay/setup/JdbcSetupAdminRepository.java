package com.imgltd.mmpay.setup;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSetupAdminRepository implements SetupAdminRepository {
  private final JdbcTemplate jdbcTemplate;

  public JdbcSetupAdminRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public boolean adminExists() {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_user WHERE role = 'admin' AND kind = 'user'", Integer.class);
    return count != null && count > 0;
  }

  @Override
  public void createAdmin(BootstrapAdminRecord admin) {
    jdbcTemplate.update(
        "INSERT INTO sys_user (id, username, kind, password_hash, role) VALUES (?, ?, 'user', ?, 'admin')",
        admin.id(),
        admin.username(),
        admin.passwordHash());
    jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role) VALUES (?, 'admin')", admin.id());
  }
}
