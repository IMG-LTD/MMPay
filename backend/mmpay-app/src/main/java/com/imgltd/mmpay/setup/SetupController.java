package com.imgltd.mmpay.setup;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class SetupController {
  private final SetupAdminRepository repository;
  private final SetupBootstrapService bootstrapService;
  private final SetupRateLimiter rateLimiter;
  private final SetupTokenService tokenService;

  public SetupController(
      SetupAdminRepository repository,
      SetupBootstrapService bootstrapService,
      SetupRateLimiter rateLimiter,
      SetupTokenService tokenService) {
    this.repository = repository;
    this.bootstrapService = bootstrapService;
    this.rateLimiter = rateLimiter;
    this.tokenService = tokenService;
  }

  @GetMapping(value = "/setup", produces = MediaType.TEXT_HTML_VALUE)
  public String getSetup(Model model, Locale locale) {
    if (!setupAvailable()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "setup_not_available");
    }
    model.addAttribute("copy", SetupPageCopy.from(locale));
    model.addAttribute("errors", Map.of());
    return "setup";
  }

  @PostMapping("/setup")
  public Object submitSetup(
      @RequestHeader(name = "X-Setup-Token", required = false) String headerToken,
      @RequestParam(name = "setupToken", required = false) String formToken,
      @ModelAttribute SetupRequest request,
      HttpServletRequest servletRequest,
      HttpServletResponse servletResponse,
      Locale locale,
      Model model) {
    if (!setupAvailable()) {
      return ResponseEntity.notFound().build();
    }
    if (!rateLimiter.tryAcquire(servletRequest.getRemoteAddr())) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }
    var token = headerToken != null && !headerToken.isBlank() ? headerToken : formToken;
    model.addAttribute("copy", SetupPageCopy.from(locale));
    if (!tokenService.matches(token)) {
      model.addAttribute("errors", Map.of("setupToken", "invalid_setup_token"));
      servletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
      return "setup";
    }
    try {
      tokenService.verify(token);
      bootstrapService.createAdmin(request);
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/login")).build();
    } catch (SetupAlreadyCompletedException exception) {
      return ResponseEntity.notFound().build();
    } catch (SetupValidationException exception) {
      model.addAttribute("errors", exception.errors());
      servletResponse.setStatus(HttpStatus.BAD_REQUEST.value());
      return "setup";
    }
  }

  private boolean setupAvailable() {
    return tokenService.setupAvailable() && !repository.adminExists();
  }
}
