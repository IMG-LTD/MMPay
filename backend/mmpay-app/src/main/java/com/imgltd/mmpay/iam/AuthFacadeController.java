package com.imgltd.mmpay.iam;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthFacadeController {
  private static final String SUCCESS_CODE = "0000";
  private static final String UNAUTHORIZED_CODE = "1001";

  private final OpaqueTokenService tokenService;
  private final JdbcIamRepository repository;

  public AuthFacadeController(OpaqueTokenService tokenService, JdbcIamRepository repository) {
    this.tokenService = tokenService;
    this.repository = repository;
  }

  @PostMapping("/auth/login")
  public ResponseEntity<Envelope<LoginPayload>> login(@RequestBody LoginRequest request) {
    try {
      var token = tokenService.loginAdminConsole(request.userName(), request.password());
      return ResponseEntity.ok(Envelope.ok(LoginPayload.from(token)));
    } catch (ResponseStatusException exception) {
      return ResponseEntity.ok(Envelope.fail(UNAUTHORIZED_CODE, "invalid_credentials"));
    }
  }

  @PostMapping("/auth/refreshToken")
  public ResponseEntity<Envelope<LoginPayload>> refresh(@RequestBody RefreshRequest request) {
    try {
      var token = tokenService.refreshAdminConsole(request.refreshToken());
      return ResponseEntity.ok(Envelope.ok(LoginPayload.from(token)));
    } catch (ResponseStatusException exception) {
      return ResponseEntity.ok(Envelope.fail(UNAUTHORIZED_CODE, "invalid_refresh_token"));
    }
  }

  @GetMapping("/auth/getUserInfo")
  public ResponseEntity<Envelope<UserInfoPayload>> userInfo() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null
        || "anonymousUser".equals(authentication.getPrincipal())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Envelope.fail(UNAUTHORIZED_CODE, "unauthenticated"));
    }
    var username = authentication.getName();
    var record = repository.findUser(username).orElseThrow(
        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user_not_found"));
    var role = record.role().toUpperCase();
    return ResponseEntity.ok(
        Envelope.ok(new UserInfoPayload(record.username(), record.username(), List.of(role), List.of())));
  }

  public record LoginRequest(String userName, String password) {}

  public record RefreshRequest(String refreshToken) {}

  public record LoginPayload(String token, String refreshToken) {
    static LoginPayload from(TokenResponse response) {
      return new LoginPayload(response.accessToken(), response.refreshToken());
    }
  }

  public record UserInfoPayload(
      @JsonProperty("userId") String userId,
      @JsonProperty("userName") String userName,
      List<String> roles,
      List<String> buttons) {}

  public record Envelope<T>(String code, String msg, T data) {
    static <T> Envelope<T> ok(T data) {
      return new Envelope<>(SUCCESS_CODE, "", data);
    }

    static <T> Envelope<T> fail(String code, String msg) {
      return new Envelope<>(code, msg, null);
    }
  }
}
