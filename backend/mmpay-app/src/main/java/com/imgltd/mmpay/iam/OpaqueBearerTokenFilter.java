package com.imgltd.mmpay.iam;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class OpaqueBearerTokenFilter extends OncePerRequestFilter {
  private static final String BEARER_PREFIX = "Bearer ";
  private final OpaqueTokenService tokenService;

  public OpaqueBearerTokenFilter(OpaqueTokenService tokenService) {
    this.tokenService = tokenService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var authorization = request.getHeader("Authorization");
    if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
      tokenService.authenticate(authorization.substring(BEARER_PREFIX.length())).ifPresent(this::authenticate);
    }
    chain.doFilter(request, response);
  }

  private void authenticate(ServicePrincipalAuthentication authentication) {
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication.toToken());
    SecurityContextHolder.setContext(context);
  }
}
