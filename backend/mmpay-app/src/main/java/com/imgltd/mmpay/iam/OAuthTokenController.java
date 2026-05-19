package com.imgltd.mmpay.iam;

import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OAuthTokenController {
  private final OpaqueTokenService tokenService;

  public OAuthTokenController(OpaqueTokenService tokenService) {
    this.tokenService = tokenService;
  }

  @PostMapping(path = "/oauth2/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public TokenResponse token(
      @RequestHeader(name = "Authorization", required = false) String authorization,
      @RequestParam MultiValueMap<String, String> form) {
    return tokenService.issue(authorization, form);
  }
}
