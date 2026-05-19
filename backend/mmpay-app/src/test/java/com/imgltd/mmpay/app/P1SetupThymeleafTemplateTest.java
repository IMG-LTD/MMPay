package com.imgltd.mmpay.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.imgltd.mmpay.setup.BootstrapAdminProperties;
import com.imgltd.mmpay.setup.SetupTokenService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ClassUtils;

@SpringBootTest(
    properties = {
      "spring.datasource.hikari.connection-init-sql=",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:/test-iam-schema.sql"
    })
@AutoConfigureMockMvc
@Import({
  TestAuditChainConfiguration.class,
  TestSetupBootstrapLockConfiguration.class,
  P1SetupThymeleafTemplateTest.FixedSetupTokenConfiguration.class
})
class P1SetupThymeleafTemplateTest {
  @Autowired private ApplicationContext context;
  @Autowired private MockMvc mockMvc;

  @Test
  void setupPageIsRenderedFromAThymeleafTemplate() throws Exception {
    assertThat(ClassUtils.isPresent("org.thymeleaf.spring6.SpringTemplateEngine", getClass().getClassLoader())).isTrue();
    assertThat(context.getResource("classpath:/templates/setup.html").exists()).isTrue();

    mockMvc
        .perform(get("/setup"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<form")))
        .andExpect(content().string(containsString("name=\"username\"")))
        .andExpect(content().string(containsString("name=\"passwordConfirm\"")))
        .andExpect(content().string(containsString("name=\"initialLocale\"")));
  }

  @Test
  void setupPageUsesAcceptLanguageForSupportedLocales() throws Exception {
    mockMvc
        .perform(get("/setup").header("Accept-Language", "zh-CN,zh;q=0.9"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("lang=\"zh-CN\"")))
        .andExpect(content().string(containsString("用户名")))
        .andExpect(content().string(containsString("创建管理员")));

    mockMvc
        .perform(get("/setup").header("Accept-Language", "en-US,en;q=0.9"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("lang=\"en-US\"")))
        .andExpect(content().string(containsString("Username")))
        .andExpect(content().string(containsString("Create admin")));
  }

  @TestConfiguration
  static class FixedSetupTokenConfiguration {
    @Bean
    @Primary
    SetupTokenService fixedSetupTokenService() {
      return SetupTokenService.create(false, BootstrapAdminProperties.empty(), this::tokenBytes);
    }

    private byte[] tokenBytes() {
      return "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    }
  }
}
