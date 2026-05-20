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
              entry("subheading", "Create the first administrator to finish provisioning."),
              entry("tokenLabel", "Setup token"),
              entry("tokenHelp",
                  "Paste the token printed in the container logs (look for \"mmpay-setup-token:\")."),
              entry("usernameLabel", "Username"),
              entry("usernameHelp", "Lowercase letters, digits, dot, dash or underscore. 3-64 chars."),
              entry("passwordLabel", "Password"),
              entry("passwordHelp", "At least 8 characters."),
              entry("passwordConfirmLabel", "Confirm password"),
              entry("initialLocaleLabel", "Initial locale"),
              entry("submitLabel", "Create admin"),
              entry("invalidTokenError", "Setup token does not match. Check the container logs."),
              entry("usernameError", "Invalid username."),
              entry("passwordError", "Invalid password."),
              entry("passwordConfirmError", "Passwords do not match."),
              entry("initialLocaleError", "Invalid locale.")));
  private static final SetupPageCopy ZH_CN =
      new SetupPageCopy(
          Map.ofEntries(
              entry("lang", "zh-CN"),
              entry("title", "MMPay 初始化"),
              entry("heading", "MMPay 初始化"),
              entry("subheading", "创建首个管理员，完成系统初始化。"),
              entry("tokenLabel", "初始化令牌"),
              entry("tokenHelp", "粘贴容器日志中打印的令牌（搜索 \"mmpay-setup-token:\"）。"),
              entry("usernameLabel", "用户名"),
              entry("usernameHelp", "小写字母、数字、点号、连字符或下划线，3-64 位。"),
              entry("passwordLabel", "密码"),
              entry("passwordHelp", "至少 8 位字符。"),
              entry("passwordConfirmLabel", "确认密码"),
              entry("initialLocaleLabel", "初始语言"),
              entry("submitLabel", "创建管理员"),
              entry("invalidTokenError", "初始化令牌不匹配，请核对容器日志。"),
              entry("usernameError", "用户名不合法。"),
              entry("passwordError", "密码不合法。"),
              entry("passwordConfirmError", "两次输入的密码不一致。"),
              entry("initialLocaleError", "语言不合法。")));

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

  public String lang() { return label("lang"); }

  public String title() { return label("title"); }

  public String heading() { return label("heading"); }

  public String subheading() { return label("subheading"); }

  public String tokenLabel() { return label("tokenLabel"); }

  public String tokenHelp() { return label("tokenHelp"); }

  public String usernameLabel() { return label("usernameLabel"); }

  public String usernameHelp() { return label("usernameHelp"); }

  public String passwordLabel() { return label("passwordLabel"); }

  public String passwordHelp() { return label("passwordHelp"); }

  public String passwordConfirmLabel() { return label("passwordConfirmLabel"); }

  public String initialLocaleLabel() { return label("initialLocaleLabel"); }

  public String submitLabel() { return label("submitLabel"); }

  public String invalidTokenError() { return label("invalidTokenError"); }

  public String usernameError() { return label("usernameError"); }

  public String passwordError() { return label("passwordError"); }

  public String passwordConfirmError() { return label("passwordConfirmError"); }

  public String initialLocaleError() { return label("initialLocaleError"); }

  private String label(String name) {
    return labels.get(name);
  }
}
