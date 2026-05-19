package com.imgltd.mmpay.license;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LicenseRelaySecurityContractTest {
  private HttpServer server;
  private byte[] received = new byte[0];

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void forwardsOpaqueBytesWithSha256AndBoundedResponseCapture() throws Exception {
    startServer(200, "0123456789abcdef");
    byte[] payload = "opaque-vendor-signed-license".getBytes(StandardCharsets.UTF_8);
    LicenseRelay relay = new LicenseRelay(new RelayConfig(Map.of("relay-prod", serverUri()), 8192));

    LicenseRelayReceipt receipt = relay.forward(payload, "relay-prod", "req-001");

    assertArrayEquals(payload, received);
    assertTrue(receipt.success());
    assertEquals(200, receipt.httpStatus());
    assertEquals(payload.length, receipt.byteCount());
    assertEquals(sha256(payload), receipt.payloadSha256());
    assertEquals(16, receipt.responseSizeBytes());
    assertFalse(receipt.responseTruncated());
  }

  @Test
  void rejectsRedirectsAndNeverTreatsThemAsDelivered() throws Exception {
    startServer(302, "redirect");
    LicenseRelay relay = new LicenseRelay(new RelayConfig(Map.of("relay-prod", serverUri()), 8192));

    LicenseRelayReceipt receipt = relay.forward("license".getBytes(StandardCharsets.UTF_8), "relay-prod", "req-002");

    assertFalse(receipt.success());
    assertEquals("redirect_attempted", receipt.errorClass());
  }

  private void startServer(int status, String response) throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/relay",
        exchange -> {
          received = exchange.getRequestBody().readAllBytes();
          byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
  }

  private URI serverUri() {
    return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/relay");
  }

  private static String sha256(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }
}
