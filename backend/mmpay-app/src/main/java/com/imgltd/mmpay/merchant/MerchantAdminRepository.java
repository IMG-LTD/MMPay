package com.imgltd.mmpay.merchant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

public final class MerchantAdminRepository {
  private static final String TENANT_ID = "default";
  private final JdbcTemplate jdbcTemplate;

  public MerchantAdminRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public MerchantRow insertMerchant(MerchantCreateRequest request, CredentialBindingResult binding, Instant now) {
    try {
      jdbcTemplate.update(
          "INSERT INTO merchants (row_uid, id, display_name, credential_handle, credential_ref, "
              + "credential_fingerprint, status, tenant_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 'active', ?, ?, ?)",
          UUID.randomUUID().toString(),
          request.id(),
          request.displayName(),
          binding.credentialRef(),
          binding.credentialRef(),
          binding.fingerprint(),
          TENANT_ID,
          Timestamp.from(now),
          Timestamp.from(now));
    } catch (DuplicateKeyException exception) {
      throw AdminProblems.conflict(AdminProblems.ACTIVE_ID_COLLISION, "active merchant id exists");
    }
    return requireMerchant(request.id());
  }

  public List<MerchantRow> listMerchants(String status, PageWindow page) {
    if (page.cursor() == null) {
      return jdbcTemplate.query(
          "SELECT * FROM merchants WHERE tenant_id = ? AND status = ? ORDER BY created_at ASC, id ASC LIMIT ?",
          this::merchant,
          TENANT_ID,
          status,
          page.limit() + 1);
    }
    return listMerchantsAfter(status, page);
  }

  public MerchantRow requireMerchant(String id) {
    return findMerchant(id).orElseThrow(() -> AdminProblems.conflict(AdminProblems.ARCHIVED_TARGET, "merchant not found"));
  }

  public Optional<MerchantRow> findMerchant(String id) {
    var rows =
        jdbcTemplate.query(
            "SELECT * FROM merchants WHERE tenant_id = ? AND id = ? AND status <> 'archived' ORDER BY created_at DESC LIMIT 1",
            this::merchant,
            TENANT_ID,
            id);
    return rows.stream().findFirst();
  }

  public ChannelRow insertChannel(String merchantId, ChannelCreateRequest request, CredentialBindingResult binding, Instant now) {
    if (findMerchant(merchantId).isEmpty()) {
      throw AdminProblems.conflict(AdminProblems.FK_ARCHIVED_MERCHANT, "merchant is archived or missing");
    }
    try {
      insertChannelRow(merchantId, request, binding, now);
    } catch (DuplicateKeyException exception) {
      throw AdminProblems.conflict(AdminProblems.ACTIVE_ID_COLLISION, "active channel id exists");
    }
    return requireChannel(request.id());
  }

  public List<ChannelRow> listChannels(String merchantId, String status, PageWindow page) {
    if (page.cursor() == null) {
      return jdbcTemplate.query(
          "SELECT * FROM channels WHERE tenant_id = ? AND merchant_id = ? AND status = ? "
              + "ORDER BY created_at ASC, id ASC LIMIT ?",
          this::channel,
          TENANT_ID,
          merchantId,
          status,
          page.limit() + 1);
    }
    return listChannelsAfter(merchantId, status, page);
  }

  public ChannelRow requireChannel(String id) {
    var rows =
        jdbcTemplate.query(
            "SELECT * FROM channels WHERE tenant_id = ? AND id = ? AND status <> 'archived' ORDER BY created_at DESC LIMIT 1",
            this::channel,
            TENANT_ID,
            id);
    return rows.stream().findFirst().orElseThrow(() -> AdminProblems.conflict(AdminProblems.ARCHIVED_TARGET, "channel not found"));
  }

  private List<MerchantRow> listMerchantsAfter(String status, PageWindow page) {
    return jdbcTemplate.query(
        "SELECT * FROM merchants WHERE tenant_id = ? AND status = ? AND (created_at > ? OR (created_at = ? AND id > ?)) "
            + "ORDER BY created_at ASC, id ASC LIMIT ?",
        this::merchant,
        TENANT_ID,
        status,
        Timestamp.from(page.cursor().createdAt()),
        Timestamp.from(page.cursor().createdAt()),
        page.cursor().id(),
        page.limit() + 1);
  }

  private List<ChannelRow> listChannelsAfter(String merchantId, String status, PageWindow page) {
    return jdbcTemplate.query(
        "SELECT * FROM channels WHERE tenant_id = ? AND merchant_id = ? AND status = ? "
            + "AND (created_at > ? OR (created_at = ? AND id > ?)) ORDER BY created_at ASC, id ASC LIMIT ?",
        this::channel,
        TENANT_ID,
        merchantId,
        status,
        Timestamp.from(page.cursor().createdAt()),
        Timestamp.from(page.cursor().createdAt()),
        page.cursor().id(),
        page.limit() + 1);
  }

  private void insertChannelRow(String merchantId, ChannelCreateRequest request, CredentialBindingResult binding, Instant now) {
    jdbcTemplate.update(
        "INSERT INTO channels (row_uid, id, merchant_id, display_name, provider_code, credential_handle, "
            + "credential_ref, credential_fingerprint, status, tenant_id, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'active', ?, ?, ?)",
        UUID.randomUUID().toString(),
        request.id(),
        merchantId,
        request.displayName(),
        request.providerCode(),
        binding.credentialRef(),
        binding.credentialRef(),
        binding.fingerprint(),
        TENANT_ID,
        Timestamp.from(now),
        Timestamp.from(now));
  }

  private MerchantRow merchant(ResultSet resultSet, int rowNumber) throws SQLException {
    return new MerchantRow(
        resultSet.getString("id"),
        resultSet.getString("display_name"),
        resultSet.getString("credential_ref"),
        resultSet.getString("credential_fingerprint"),
        resultSet.getString("status"),
        instant(resultSet, "created_at"),
        instant(resultSet, "updated_at"));
  }

  private ChannelRow channel(ResultSet resultSet, int rowNumber) throws SQLException {
    return new ChannelRow(
        resultSet.getString("id"),
        resultSet.getString("merchant_id"),
        resultSet.getString("display_name"),
        resultSet.getString("provider_code"),
        resultSet.getString("credential_ref"),
        resultSet.getString("credential_fingerprint"),
        resultSet.getString("status"),
        instant(resultSet, "created_at"),
        instant(resultSet, "updated_at"));
  }

  private Instant instant(ResultSet resultSet, String name) throws SQLException {
    return resultSet.getTimestamp(name).toInstant();
  }
}
