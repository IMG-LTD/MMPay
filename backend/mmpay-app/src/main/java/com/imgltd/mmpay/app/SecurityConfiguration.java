package com.imgltd.mmpay.app;

import jakarta.servlet.http.HttpServletResponse;
import com.imgltd.mmpay.iam.OpaqueBearerTokenFilter;
import com.imgltd.mmpay.iam.OpaqueTokenService;
import com.imgltd.mmpay.iam.JdbcIamRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
  private static final String[] HUMAN_ROLES = {"ADMIN", "OPS", "FINANCE", "AUDITOR"};
  private static final String[] AUDIT_ROLES = {"ADMIN", "AUDITOR"};

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, OpaqueTokenService tokenService) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .requestMatchers("/actuator/env/**", "/actuator/configprops/**", "/actuator/metrics/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/admin/audit/**")
                    .hasAnyRole(AUDIT_ROLES)
                    .requestMatchers("/api/admin/**")
                    .hasAnyRole(HUMAN_ROLES)
                    .anyRequest()
                    .permitAll())
        .exceptionHandling(
            exceptions ->
                exceptions.authenticationEntryPoint(
                    (request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
        .addFilterBefore(new OpaqueBearerTokenFilter(tokenService), AnonymousAuthenticationFilter.class)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable);
    return http.build();
  }

  @Bean
  UserDetailsService jdbcUserDetailsService(JdbcIamRepository repository) {
    return username ->
        repository
            .findUser(username)
            .filter(user -> "user".equals(user.kind()))
            .map(user -> User.withUsername(user.username()).password(user.passwordHash()).roles(user.role().toUpperCase()).build())
            .orElseThrow(() -> new UsernameNotFoundException("iam_user_not_found"));
  }
}
