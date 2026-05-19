package com.imgltd.mmpay.setup;

import java.util.regex.Pattern;

public record BootstrapAdminProperties(String username, String passwordHash) {
  private static final Pattern BCRYPT_HASH = Pattern.compile("^\\$2[aby]\\$\\d\\d\\$.{53}$");
  private static final Pattern USERNAME = Pattern.compile("^[a-z][a-z0-9._-]{2,63}$");

  public static BootstrapAdminProperties empty() {
    return new BootstrapAdminProperties(null, null);
  }

  public boolean configured() {
    return username != null || passwordHash != null;
  }

  public void validate() {
    if (!configured()) {
      return;
    }
    if (username == null || passwordHash == null || !USERNAME.matcher(username).matches()) {
      throw new IllegalArgumentException("bootstrap_admin_invalid");
    }
    if (!BCRYPT_HASH.matcher(passwordHash).matches()) {
      throw new IllegalArgumentException("bootstrap_hash_invalid");
    }
  }
}
