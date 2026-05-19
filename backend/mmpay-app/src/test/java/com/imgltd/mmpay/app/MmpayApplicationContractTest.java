package com.imgltd.mmpay.app;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
      "spring.cloud.nacos.discovery.enabled=false",
      "spring.cloud.nacos.config.enabled=false",
      "spring.datasource.hikari.connection-init-sql=",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:/test-iam-schema.sql"
    })
@AutoConfigureMockMvc
@Import(TestAuditChainConfiguration.class)
class MmpayApplicationContractTest {
  @Autowired private MockMvc mockMvc;

  @Test
  @WithMockUser(roles = "OPS")
  void exposesHealthAndAdminDashboardFromTheAppEntryPoint() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());

    mockMvc
        .perform(get("/api/admin/dashboard"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.navigation[0].key", is("merchants")))
        .andExpect(jsonPath("$.tables[1].rows[0].status", is("credentials-required")))
        .andExpect(jsonPath("$.tables[1].rows[0].reason", is("live provider client is not wired")))
        .andExpect(jsonPath("$.tables[2].rows").isEmpty())
        .andExpect(jsonPath("$.tables[4].rows[0].reason", is("unsupported by current provider")));
  }
}
