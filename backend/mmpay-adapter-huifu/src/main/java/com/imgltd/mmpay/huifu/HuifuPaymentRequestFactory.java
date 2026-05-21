package com.imgltd.mmpay.huifu;

import com.imgltd.mmpay.adapter.ProviderPaymentRequest;
import com.imgltd.mmpay.adapter.ProviderRefundRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class HuifuPaymentRequestFactory {
  private static final String DEFAULT_TRADE_TYPE = "A_NATIVE";
  private static final String LIGHTNING_SDK_VERSION = "javaSDK_lightning_1.0.5";
  private static final DateTimeFormatter REQUEST_DATE = DateTimeFormatter.BASIC_ISO_DATE;

  private final HuifuSandboxCredentials credentials;

  public HuifuPaymentRequestFactory(HuifuSandboxCredentials credentials) {
    this.credentials = Objects.requireNonNull(credentials, "credentials");
  }

  public HuifuSignedRequest createAggregationNativePayment(
      ProviderPaymentRequest request, LocalDate requestDate, String requestSequenceId) {
    Map<String, Object> data = paymentData(request, requestDate, requestSequenceId);
    String sign = HuifuRsaSigner.signData(data, credentials.rsaPrivateKey());
    return new HuifuSignedRequest(envelope(data, sign), headers(data), data, sign);
  }

  public HuifuSignedRequest queryAggregationPayment(
      String originalRequestDate, String originalRequestSequenceId) {
    Map<String, Object> data = queryData(originalRequestDate, originalRequestSequenceId);
    String sign = HuifuRsaSigner.signData(data, credentials.rsaPrivateKey());
    return new HuifuSignedRequest(envelope(data, sign), headers(data), data, sign);
  }

  public HuifuSignedRequest refundAggregationPayment(
      ProviderRefundRequest request,
      LocalDate requestDate,
      String requestSequenceId,
      String originalRequestDate,
      String originalRequestSequenceId) {
    Objects.requireNonNull(request, "request");
    Map<String, Object> data =
        refundData(requestDate, requestSequenceId, originalRequestDate, originalRequestSequenceId);
    data.put("ord_amt", formatAmount(request.amountMinor()));
    data.put("remark", request.reason());
    data.put("notify_url", credentials.notifyUrl());
    String sign = HuifuRsaSigner.signData(data, credentials.rsaPrivateKey());
    return new HuifuSignedRequest(envelope(data, sign), headers(data), data, sign);
  }

  private Map<String, Object> paymentData(
      ProviderPaymentRequest request, LocalDate requestDate, String requestSequenceId) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(requestDate, "requestDate");
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("goods_desc", request.subject());
    data.put("huifu_id", credentials.merchantId());
    data.put("notify_url", credentials.notifyUrl());
    data.put("req_date", requestDate.format(REQUEST_DATE));
    data.put("req_seq_id", requireText(requestSequenceId, "requestSequenceId"));
    data.put("trade_type", DEFAULT_TRADE_TYPE);
    data.put("trans_amt", formatAmount(request.amountMinor()));
    return data;
  }

  private Map<String, Object> queryData(
      String originalRequestDate, String originalRequestSequenceId) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("huifu_id", credentials.merchantId());
    data.put("req_date", requireText(originalRequestDate, "originalRequestDate"));
    data.put("req_seq_id", requireText(originalRequestSequenceId, "originalRequestSequenceId"));
    return data;
  }

  private Map<String, Object> refundData(
      LocalDate requestDate,
      String requestSequenceId,
      String originalRequestDate,
      String originalRequestSequenceId) {
    Objects.requireNonNull(requestDate, "requestDate");
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("huifu_id", credentials.merchantId());
    data.put("req_date", requestDate.format(REQUEST_DATE));
    data.put("req_seq_id", requireText(requestSequenceId, "requestSequenceId"));
    data.put("org_req_date", requireText(originalRequestDate, "originalRequestDate"));
    data.put("org_req_seq_id", requireText(originalRequestSequenceId, "originalRequestSequenceId"));
    return data;
  }

  private Map<String, Object> envelope(Map<String, Object> data, String sign) {
    Map<String, Object> envelope = new LinkedHashMap<>();
    envelope.put("sys_id", credentials.sysId());
    envelope.put("product_id", credentials.productId());
    envelope.put("data", data);
    envelope.put("sign", sign);
    return envelope;
  }

  private Map<String, String> headers(Map<String, Object> data) {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("sdk_version", LIGHTNING_SDK_VERSION);
    headers.put("jpt-sdk_version", LIGHTNING_SDK_VERSION);
    headers.put("sys_id", credentials.sysId());
    headers.put("jpt-sys_id", credentials.sysId());
    headers.put("jpt-x-skill-source", credentials.skillSource());
    headers.put("jpt-x-skill-huifu_id", data.get("huifu_id").toString());
    return headers;
  }

  private static String formatAmount(long amountMinor) {
    return BigDecimal.valueOf(amountMinor, 2).setScale(2, RoundingMode.UNNECESSARY).toPlainString();
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value;
  }
}
