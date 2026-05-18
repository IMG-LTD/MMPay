package com.imgltd.mmpay.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

class AdminDashboardControllerTest {
  @Test
  void dashboardExposesMp5OperationsWithoutFakeSuccessRows() throws NoSuchMethodException {
    AdminDashboardController controller = new AdminDashboardController(new AdminDashboardReadService());

    AdminDashboardResponse response = controller.getDashboard();

    GetMapping mapping = AdminDashboardController.class.getMethod("getDashboard").getAnnotation(GetMapping.class);
    assertEquals("/api/admin/dashboard", mapping.value()[0]);
    assertEquals(
        List.of(
            "merchants",
            "channels",
            "orders",
            "refunds",
            "invoices",
            "webhook-logs",
            "reconciliation",
            "external-readiness"),
        response.navigation().stream().map(AdminDashboardResponse.NavigationItem::key).toList());
    assertEquals(List.of("0", "0", "0", "0"), response.metrics().stream().map(AdminDashboardResponse.MetricCard::value).toList());
    assertEquals("credentials-required", response.table("channels").rows().getFirst().get("status"));
    assertEquals("live provider client is not wired", response.table("channels").rows().getFirst().get("reason"));
    assertEquals("unsupported by current provider", response.table("invoices").rows().getFirst().get("reason"));
    assertEquals(
        "blocked",
        response.table("external-readiness").rows().getFirst().get("status"));
    assertEquals(
        "real Huifu sandbox request and callback evidence",
        response.table("external-readiness").rows().getFirst().get("requiredEvidence"));
    assertEquals("secret-handle", response.table("credentials").rows().getFirst().get("valueKind"));
    assertFalse(response.table("orders").containsStatus("succeeded"));
    assertFalse(response.table("webhook-logs").containsStatus("delivered"));
  }
}
