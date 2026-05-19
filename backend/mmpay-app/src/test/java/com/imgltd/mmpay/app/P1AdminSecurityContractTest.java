package com.imgltd.mmpay.app;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
      "spring.sql.init.schema-locations=classpath:/test-iam-schema.sql"
    })
@AutoConfigureMockMvc
@Import(TestAuditChainConfiguration.class)
class P1AdminSecurityContractTest {
  @Autowired private MockMvc mockMvc;

  @Test
  void adminApiRejectsUnauthenticatedRequests() throws Exception {
    mockMvc
        .perform(get("/api/admin/dashboard"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().doesNotExist("WWW-Authenticate"));
    mockMvc.perform(get("/api/admin/audit")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/admin/audit/verify")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "AUDITOR")
  void auditorCanReadAuditEventsAndVerifyTheChain() throws Exception {
    mockMvc.perform(get("/api/admin/audit")).andExpect(status().isOk()).andExpect(jsonPath("$.events.length()", greaterThanOrEqualTo(1)));

    mockMvc
        .perform(get("/api/admin/audit/verify"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok", is(true)))
        .andExpect(jsonPath("$.from_id", is(1)));
  }

  @Test
  @WithMockUser(roles = "FINANCE")
  void financeCannotReadAuditEventsOrVerifyTheChain() throws Exception {
    mockMvc.perform(get("/api/admin/audit")).andExpect(status().isForbidden());
    mockMvc.perform(get("/api/admin/audit/verify")).andExpect(status().isForbidden());
  }
}
