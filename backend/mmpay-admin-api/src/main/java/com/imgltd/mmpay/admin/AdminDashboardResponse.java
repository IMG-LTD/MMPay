package com.imgltd.mmpay.admin;

import java.util.List;
import java.util.Map;

public record AdminDashboardResponse(
    List<NavigationItem> navigation, List<MetricCard> metrics, List<AdminTable> tables) {
  public AdminTable table(String key) {
    return tables.stream()
        .filter(table -> table.key().equals(key))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("unknown admin table: " + key));
  }

  public record NavigationItem(String key, String label) {}

  public record MetricCard(String label, String value) {}

  public record AdminTable(String key, List<String> columns, List<Map<String, String>> rows) {
    public boolean containsStatus(String status) {
      return rows.stream().anyMatch(row -> status.equals(row.get("status")));
    }
  }
}
