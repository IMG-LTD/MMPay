package com.imgltd.mmpay.app;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.hikari.connection-init-sql=",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:/test-iam-schema.sql",
      "MMPAY_AUDIT_HMAC_KEY=raw-audit-key"
    })
@AutoConfigureMockMvc
@Import(TestAuditChainConfiguration.class)
class ActuatorHardeningTest {
  @Autowired private MockMvc mockMvc;

  @Test
  void healthAndInfoArePublicButSensitiveActuatorEndpointsRequireAdmin() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
    mockMvc.perform(get("/actuator/env/MMPAY_AUDIT_HMAC_KEY")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/actuator/configprops")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminCanReadSanitizedActuatorEnvironment() throws Exception {
    mockMvc
        .perform(get("/actuator/env/MMPAY_AUDIT_HMAC_KEY"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("raw-audit-key"))));
  }
}
