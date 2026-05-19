package com.imgltd.mmpay.setup;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SetupFormValidator {
  private static final int PASSWORD_MAX_LENGTH = 128;
  private static final int PASSWORD_MIN_LENGTH = 8;
  private static final Pattern USERNAME = Pattern.compile("^[a-z][a-z0-9._-]{2,63}$");
  private static final Set<String> LOCALES = Set.of("zh-CN", "en-US");

  public ValidatedSetupForm validate(SetupRequest request) {
    var errors = new LinkedHashMap<String, String>();
    validateUsername(request.username(), errors);
    validatePassword(request.password(), request.passwordConfirm(), errors);
    validateLocale(request.initialLocale(), errors);
    if (!errors.isEmpty()) {
      throw new SetupValidationException(errors);
    }
    return new ValidatedSetupForm(request.username(), request.password(), request.initialLocale());
  }

  private static void validateUsername(String username, Map<String, String> errors) {
    if (username == null || !USERNAME.matcher(username).matches()) {
      errors.put("username", "username_invalid");
    }
  }

  private static void validatePassword(String password, String passwordConfirm, Map<String, String> errors) {
    if (password == null || password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
      errors.put("password", "password_policy_mismatch");
    }
    if (password == null || !password.equals(passwordConfirm)) {
      errors.put("passwordConfirm", "password_confirm_mismatch");
    }
  }

  private static void validateLocale(String locale, Map<String, String> errors) {
    if (!LOCALES.contains(locale)) {
      errors.put("initialLocale", "locale_invalid");
    }
  }
}
