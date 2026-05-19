package com.imgltd.mmpay.license;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LicenseRelayTest {
  @Test
  void forwardsOpaqueVendorSignedLicenseBytesWithoutParsingClaims() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/relay", exchange -> {
      byte[] payload = exchange.getRequestBody().readAllBytes();
      exchange.sendResponseHeaders(202, payload.length);
      exchange.getResponseBody().write(payload);
      exchange.close();
    });
    server.start();
    try {
      URI targetUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/relay");
      LicenseRelay relay = new LicenseRelay(new RelayConfig(Map.of("target-001", targetUri), 1024));
      byte[] signedLicense = "opaque-vendor-signed-license-bytes".getBytes(StandardCharsets.UTF_8);

      LicenseRelayReceipt receipt = relay.forward(signedLicense, "target-001", "request-001");

      assertTrue(receipt.success());
      assertEquals("target-001", receipt.targetId());
      assertEquals(signedLicense.length, receipt.byteCount());
      assertEquals(202, receipt.httpStatus());
    } finally {
      server.stop(0);
    }
  }
}
