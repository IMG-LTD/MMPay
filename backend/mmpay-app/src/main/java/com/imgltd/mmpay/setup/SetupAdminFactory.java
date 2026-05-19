package com.imgltd.mmpay.setup;

import java.util.function.Supplier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SetupAdminFactory {
  private final Supplier<String> idSource;
  private final PasswordEncoder passwordEncoder;
  private final SetupFormValidator validator;

  public SetupAdminFactory(
      Supplier<String> idSource, PasswordEncoder passwordEncoder, SetupFormValidator validator) {
    this.idSource = idSource;
    this.passwordEncoder = passwordEncoder;
    this.validator = validator;
  }

  public BootstrapAdminRecord create(SetupRequest request) {
    var form = validator.validate(request);
    return new BootstrapAdminRecord(
        idSource.get(), form.username(), passwordEncoder.encode(form.password()), form.initialLocale());
  }
}
