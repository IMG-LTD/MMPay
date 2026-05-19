package com.imgltd.mmpay.setup;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
    return "setup";
  }

  @PostMapping("/setup")
  public ResponseEntity<?> submitSetup(
      @RequestHeader(name = "X-Setup-Token", required = false) String token,
      SetupRequest request,
      HttpServletRequest servletRequest) {
    if (!setupAvailable()) {
      return ResponseEntity.notFound().build();
    }
    if (!rateLimiter.tryAcquire(servletRequest.getRemoteAddr())) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }
    if (!tokenService.matches(token)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    try {
      bootstrapService.createAdmin(request);
      tokenService.verify(token);
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/login")).build();
    } catch (SetupAlreadyCompletedException exception) {
      return ResponseEntity.notFound().build();
    } catch (SetupValidationException exception) {
      return ResponseEntity.badRequest().body(new SetupErrorResponse(exception.errors()));
    }
  }

  private boolean setupAvailable() {
    return tokenService.setupAvailable() && !repository.adminExists();
  }
}
