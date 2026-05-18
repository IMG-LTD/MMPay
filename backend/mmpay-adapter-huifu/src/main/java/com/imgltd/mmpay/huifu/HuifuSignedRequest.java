package com.imgltd.mmpay.huifu;

import java.util.Map;

public record HuifuSignedRequest(
    Map<String, Object> envelope, Map<String, String> headers, Map<String, Object> data, String sign) {
  public HuifuSignedRequest {
    envelope = Map.copyOf(envelope);
    headers = Map.copyOf(headers);
    data = Map.copyOf(data);
    if (sign == null || sign.isBlank()) {
      throw new IllegalArgumentException("sign must not be blank");
    }
  }
}
