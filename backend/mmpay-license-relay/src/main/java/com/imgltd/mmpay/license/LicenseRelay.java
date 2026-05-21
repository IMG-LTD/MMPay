package com.imgltd.mmpay.license;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.HexFormat;

public final class LicenseRelay {
  private final RelayConfig config;
  private final HttpClient httpClient;

  public LicenseRelay(RelayConfig config) {
    this(
        config,
        HttpClient.newBuilder()
            .connectTimeout(config.timeout())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build());
  }

  LicenseRelay(RelayConfig config, HttpClient httpClient) {
    this.config = Objects.requireNonNull(config, "config");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
  }

  public LicenseRelayReceipt forward(byte[] opaqueLicenseBytes, String targetId, String requestId) {
    if (opaqueLicenseBytes == null || opaqueLicenseBytes.length == 0) {
      throw new IllegalArgumentException("opaque license bytes must not be empty");
    }
    if (targetId == null || targetId.isBlank()) {
      throw new IllegalArgumentException("targetId must not be blank");
    }
    if (requestId == null || requestId.isBlank()) {
      throw new IllegalArgumentException("requestId must not be blank");
    }

    byte[] payload = opaqueLicenseBytes.clone();
    String payloadSha256 = sha256(payload);
    HttpRequest request =
        HttpRequest.newBuilder(config.targetUri(targetId))
            .timeout(config.timeout())
            .header("Content-Type", "application/octet-stream")
            .header("X-Request-Id", requestId)
            .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
            .build();
    return dispatch(request, targetId, requestId, payload.length, payloadSha256);
  }

  private LicenseRelayReceipt dispatch(
      HttpRequest request, String targetId, String requestId, int byteCount, String payloadSha256) {
    try {
      HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
      RelayResponseBody body;
      try (InputStream stream = response.body()) {
        body = readBody(stream);
      }
      String errorClass = classify(response.statusCode());
      return new LicenseRelayReceipt(
          targetId,
          requestId,
          byteCount,
          payloadSha256,
          response.statusCode(),
          sha256(body.bytes()),
          body.sizeBytes(),
          body.truncated(),
          errorClass,
          errorClass == null);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return failure(targetId, requestId, byteCount, payloadSha256, "dispatch_interrupted");
    } catch (IOException exception) {
      return failure(targetId, requestId, byteCount, payloadSha256, "dispatch_io");
    }
  }

  private LicenseRelayReceipt failure(
      String targetId, String requestId, int byteCount, String payloadSha256, String errorClass) {
    return new LicenseRelayReceipt(targetId, requestId, byteCount, payloadSha256, 0, null, 0, false, errorClass, false);
  }

  private RelayResponseBody readBody(InputStream body) throws IOException {
    int cap = config.responseCapBytes();
    ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(cap, 1024));
    int sizeBytes = 0;
    int next;
    while ((next = body.read()) != -1) {
      sizeBytes++;
      if (output.size() < cap) {
        output.write(next);
      }
      if (sizeBytes > cap) {
        return new RelayResponseBody(output.toByteArray(), sizeBytes, true);
      }
    }
    return new RelayResponseBody(output.toByteArray(), sizeBytes, false);
  }

  private String classify(int statusCode) {
    if (statusCode >= 200 && statusCode < 300) {
      return null;
    }
    if (statusCode >= 300 && statusCode < 400) {
      return "redirect_attempted";
    }
    if (statusCode >= 400 && statusCode < 500) {
      return "dispatch_4xx";
    }
    if (statusCode >= 500 && statusCode < 600) {
      return "dispatch_5xx";
    }
    return "unexpected_status";
  }

  private static String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("sha256 unavailable", exception);
    }
  }

  private record RelayResponseBody(byte[] bytes, int sizeBytes, boolean truncated) {}
}
