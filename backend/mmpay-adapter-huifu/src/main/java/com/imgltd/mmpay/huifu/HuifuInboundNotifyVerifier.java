package com.imgltd.mmpay.huifu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imgltd.mmpay.adapter.ProviderEvent;
import com.imgltd.mmpay.adapter.ProviderPaymentStatus;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public final class HuifuInboundNotifyVerifier {
  private static final ObjectMapper JSON = new ObjectMapper();

  private final String huifuPublicKey;

  public HuifuInboundNotifyVerifier(String huifuPublicKey) {
    this.huifuPublicKey = requireText(huifuPublicKey, "huifuPublicKey");
  }

  public ProviderEvent verify(byte[] rawBody) {
    Map<String, String> form = parseForm(rawBody);
    String respData = requireText(form.get("resp_data"), "resp_data");
    String sign = requireText(form.get("sign"), "sign");
    if (!HuifuRsaSigner.verifyRaw(respData, huifuPublicKey, sign)) {
      throw new IllegalArgumentException("Huifu notify_url signature verification failed");
    }
    return toProviderEvent(readRespData(respData));
  }

  private static ProviderEvent toProviderEvent(Map<String, String> data) {
    return new ProviderEvent(
        requireText(data.get("hf_seq_id"), "hf_seq_id"),
        requireText(data.get("req_seq_id"), "req_seq_id"),
        status(data.get("trans_stat")),
        amountMinor(data.get("trans_amt")),
        Instant.now());
  }

  private static Map<String, String> parseForm(byte[] rawBody) {
    String body = new String(rawBody, StandardCharsets.UTF_8);
    return Arrays.stream(body.split("&"))
        .map(part -> part.split("=", 2))
        .collect(Collectors.toMap(part -> decode(part[0]), part -> part.length == 2 ? decode(part[1]) : ""));
  }

  private static Map<String, String> readRespData(String respData) {
    try {
      return JSON.readValue(respData, new TypeReference<>() {});
    } catch (JsonProcessingException error) {
      throw new IllegalArgumentException("Huifu resp_data JSON parsing failed", error);
    }
  }

  private static ProviderPaymentStatus status(String value) {
    return switch (requireText(value, "trans_stat")) {
      case "S" -> ProviderPaymentStatus.SUCCEEDED;
      case "F" -> ProviderPaymentStatus.FAILED;
      default -> ProviderPaymentStatus.PENDING;
    };
  }

  private static long amountMinor(String value) {
    return new BigDecimal(requireText(value, "trans_amt")).movePointRight(2).longValueExact();
  }

  private static String decode(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value;
  }
}
