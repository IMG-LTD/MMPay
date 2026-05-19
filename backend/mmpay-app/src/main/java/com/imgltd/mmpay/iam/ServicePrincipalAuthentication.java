package com.imgltd.mmpay.iam;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record ServicePrincipalAuthentication(String principalName, String role) {
  static ServicePrincipalAuthentication from(TokenPrincipalRecord record) {
    return new ServicePrincipalAuthentication(record.principalName(), record.role());
  }

  UsernamePasswordAuthenticationToken toToken() {
    return new UsernamePasswordAuthenticationToken(
        principalName, "opaque-token", List.of(new SimpleGrantedAuthority(IamRole.authority(role))));
  }
}
