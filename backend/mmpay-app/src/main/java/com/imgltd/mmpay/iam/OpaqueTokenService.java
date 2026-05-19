package com.imgltd.mmpay.iam;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OpaqueTokenService {
  private static final Duration HUMAN_ACCESS_TOKEN_TTL = Duration.ofMinutes(30);
  private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
  private static final Duration SERVICE_TOKEN_TTL = Duration.ofHours(1);
  private static final int SECRET_BYTES = 32;
  private final Clock clock = Clock.systemUTC();
  private final JdbcIamRepository repository;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private final SecureRandom secureRandom = new SecureRandom();

  public OpaqueTokenService(JdbcIamRepository repository) {
    this.repository = repository;
  }

  public TokenResponse issue(String authorization, MultiValueMap<String, String> form) {
    var credentials = BasicClientCredentials.parse(authorization);
    var client = repository.findRegisteredClient(credentials.clientId()).orElseThrow(this::invalidClient);
    validateClient(client, credentials.clientSecret());
    return switch (required(form, "grant_type")) {
      case "client_credentials" -> issueClientCredentials(client);
      case "password" -> issuePassword(client, form);
      case "refresh_token" -> refresh(client, form);
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported_grant_type");
    };
  }

  public Optional<ServicePrincipalAuthentication> authenticate(String accessToken) {
    return repository.findAccessToken(accessToken, Instant.now(clock)).map(ServicePrincipalAuthentication::from);
  }

  private TokenResponse issueClientCredentials(RegisteredClientRecord client) {
    requireGrant(client, "client_credentials");
    var now = Instant.now(clock);
    var expiresAt = now.plus(SERVICE_TOKEN_TTL);
    var accessToken = randomSecret();
    repository.saveAuthorization(
        new TokenRecord(
            UUID.randomUUID().toString(),
            client.id(),
            client.clientId(),
            "client_credentials",
            client.scopes(),
            accessToken,
            now,
            expiresAt,
            null,
            null,
            null));
    return new TokenResponse(accessToken, "Bearer", SERVICE_TOKEN_TTL.toSeconds(), String.join(" ", client.scopes()), null);
  }

  private TokenResponse issuePassword(RegisteredClientRecord client, MultiValueMap<String, String> form) {
    requireGrant(client, "password");
    var user = repository.findUser(required(form, "username")).orElseThrow(this::invalidGrant);
    if (!"user".equals(user.kind()) || !passwordEncoder.matches(required(form, "password"), user.passwordHash())) {
      throw invalidGrant();
    }
    return issueUserTokens(client, user.username(), user.role());
  }

  private TokenResponse refresh(RegisteredClientRecord client, MultiValueMap<String, String> form) {
    requireGrant(client, "refresh_token");
    var token = repository.findRefreshToken(required(form, "refresh_token"), Instant.now(clock)).orElseThrow(this::invalidGrant);
    if (!client.id().equals(token.registeredClientId())) {
      throw invalidGrant();
    }
    repository.consumeRefreshToken(token.refreshToken());
    return issueUserTokens(client, token.principalName(), token.role());
  }

  private TokenResponse issueUserTokens(RegisteredClientRecord client, String principalName, String role) {
    var now = Instant.now(clock);
    var scopes = Set.of("role:" + role);
    var accessToken = randomSecret();
    var refreshToken = randomSecret();
    repository.saveAuthorization(
        new TokenRecord(
            UUID.randomUUID().toString(),
            client.id(),
            principalName,
            "password",
            scopes,
            accessToken,
            now,
            now.plus(HUMAN_ACCESS_TOKEN_TTL),
            refreshToken,
            now,
            now.plus(REFRESH_TOKEN_TTL)));
    return new TokenResponse(accessToken, "Bearer", HUMAN_ACCESS_TOKEN_TTL.toSeconds(), String.join(" ", scopes), refreshToken);
  }

  private void validateClient(RegisteredClientRecord client, String clientSecret) {
    if (!client.secretFingerprint().equals(IamFingerprint.sha256(clientSecret))) {
      throw invalidClient();
    }
  }

  private void requireGrant(RegisteredClientRecord client, String grant) {
    if (!client.grants().contains(grant)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unauthorized_grant_type");
    }
  }

  private String required(MultiValueMap<String, String> form, String name) {
    var value = form.getFirst(name);
    if (value == null || value.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing_" + name);
    }
    return value;
  }

  private String randomSecret() {
    var bytes = new byte[SECRET_BYTES];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private ResponseStatusException invalidClient() {
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_client");
  }

  private ResponseStatusException invalidGrant() {
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_grant");
  }
}
