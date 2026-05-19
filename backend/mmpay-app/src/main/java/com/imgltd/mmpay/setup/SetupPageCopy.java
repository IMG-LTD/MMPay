package com.imgltd.mmpay.setup;

import static java.util.Map.entry;

import java.util.Locale;
import java.util.Map;

public final class SetupPageCopy {
  private static final String LANGUAGE_ZH = "zh";
  private static final SetupPageCopy EN_US =
      new SetupPageCopy(
          Map.ofEntries(
              entry("lang", "en-US"),
              entry("title", "MMPay setup"),
              entry("heading", "MMPay setup"),
              entry("usernameLabel", "Username"),
              entry("passwordLabel", "Password"),
              entry("passwordConfirmLabel", "Confirm password"),
              entry("initialLocaleLabel", "Initial locale"),
              entry("submitLabel", "Create admin")));
  private static final SetupPageCopy ZH_CN =
      new SetupPageCopy(
          Map.ofEntries(
              entry("lang", "zh-CN"),
              entry("title", "MMPay 初始化"),
              entry("heading", "MMPay 初始化"),
              entry("usernameLabel", "用户名"),
              entry("passwordLabel", "密码"),
              entry("passwordConfirmLabel", "确认密码"),
              entry("initialLocaleLabel", "初始语言"),
              entry("submitLabel", "创建管理员")));

  private final Map<String, String> labels;

  private SetupPageCopy(Map<String, String> labels) {
    this.labels = Map.copyOf(labels);
  }

  public static SetupPageCopy from(Locale locale) {
    if (locale != null && LANGUAGE_ZH.equals(locale.getLanguage())) {
      return ZH_CN;
    }
    return EN_US;
  }

  public String lang() {
    return label("lang");
  }

  public String title() {
    return label("title");
  }

  public String heading() {
    return label("heading");
  }

  public String usernameLabel() {
    return label("usernameLabel");
  }

  public String passwordLabel() {
    return label("passwordLabel");
  }

  public String passwordConfirmLabel() {
    return label("passwordConfirmLabel");
  }

  public String initialLocaleLabel() {
    return label("initialLocaleLabel");
  }

  public String submitLabel() {
    return label("submitLabel");
  }

  private String label(String name) {
    return labels.get(name);
  }
}
