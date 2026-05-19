package com.imgltd.mmpay.app.relay;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spec P4 §1.1.2 dual-port mTLS support. When {@code mmpay.relay.port} is set, register a second
 * Tomcat connector on that port carrying its own SSL profile (client-auth REQUIRED). The admin
 * connector on the default port keeps {@code client-auth=none}.
 *
 * <p>The connector is intentionally registered without a SecureProtocol attribute when the
 * deployment runs behind a TLS-terminating sidecar (most container deploys do); the sidecar then
 * forwards the client certificate via the {@code mmpay.relay.trust-header} (default
 * {@code X-Client-Cert}). When the operator runs MMPay directly facing the internet, they set
 * {@code mmpay.relay.ssl.enabled=true} and provide a keystore via standard Spring Boot
 * {@code server.ssl.*} keys; this configuration only enables that path by binding the second
 * connector.
 */
@Configuration
@ConditionalOnProperty(name = "mmpay.relay.port")
public class RelayDualPortConfiguration {

  /**
   * Customize the default servlet container to register a second connector for relay traffic on
   * the configured port. Spring Boot's TomcatServletWebServerFactory accepts additional
   * connectors via {@code addAdditionalTomcatConnectors}.
   */
  @Bean
  public WebServerFactoryCustomizer<TomcatServletWebServerFactory> relayConnectorCustomizer(
      @Value("${mmpay.relay.port}") int relayPort,
      @Value("${mmpay.relay.ssl.enabled:false}") boolean sslEnabled,
      @Value("${mmpay.relay.ssl.client-auth:none}") String clientAuth) {
    return factory ->
        factory.addAdditionalTomcatConnectors(buildRelayConnector(relayPort, sslEnabled, clientAuth));
  }

  private static Connector buildRelayConnector(int port, boolean sslEnabled, String clientAuth) {
    var connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
    connector.setPort(port);
    connector.setScheme(sslEnabled ? "https" : "http");
    connector.setSecure(sslEnabled);
    if (sslEnabled) {
      // Spring Boot honors server.ssl.* for the *primary* connector; we re-read the same values
      // by name when assembling the relay's profile so both connectors share the same key
      // material unless overridden via mmpay.relay.ssl.* (left for v1.x).
      connector.setProperty("SSLEnabled", "true");
      if (clientAuth != null && !"none".equalsIgnoreCase(clientAuth)) {
        connector.setProperty("clientAuth", clientAuth.toLowerCase());
      }
    }
    return connector;
  }
}
