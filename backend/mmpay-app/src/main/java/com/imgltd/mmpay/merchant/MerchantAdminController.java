package com.imgltd.mmpay.merchant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class MerchantAdminController {
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private final MerchantAdminService service;
  private final IdempotencyStore idempotencyStore;
  private final ObjectMapper objectMapper;

  public MerchantAdminController(
      MerchantAdminService service, IdempotencyStore idempotencyStore, ObjectMapper objectMapper) {
    this.service = service;
    this.idempotencyStore = idempotencyStore;
    this.objectMapper = objectMapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
  }

  @PostMapping("/merchants")
  @PreAuthorize("hasRole('ADMIN')")
  ResponseEntity<Object> createMerchant(
      @RequestBody String body, @RequestHeader(value = "Idempotency-Key", required = false) String key, Authentication auth) {
    var requestHash = RequestHash.sha256(body);
    var replay = idempotencyStore.get(key, requestHash);
    if (replay != null) {
      return ResponseEntity.status(replay.status()).body(replay.body());
    }
    var request = read(body, MerchantCreateRequest.class);
    rejectTenant(body);
    var response = service.createMerchant(request, AuditActor.from(auth));
    idempotencyStore.put(key, requestHash, HttpStatus.CREATED, response);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/merchants")
  @PreAuthorize("hasAnyRole('ADMIN','OPS','FINANCE','AUDITOR')")
  ListResponse<MerchantResponse> listMerchants(
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "limit", required = false) Integer limit,
      @RequestParam(name = "cursor", required = false) String cursor) {
    return service.listMerchants(status, PageWindow.of(limit, cursor));
  }

  @GetMapping("/merchants/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','OPS','FINANCE','AUDITOR')")
  MerchantResponse getMerchant(@PathVariable("id") String id) {
    return service.getMerchant(id);
  }

  @PatchMapping("/merchants/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  MerchantResponse patchMerchant(@PathVariable("id") String id, @RequestBody String body, Authentication auth) {
    var request = read(body, MerchantPatchRequest.class);
    rejectTenant(body);
    return service.patchMerchant(id, request, AuditActor.from(auth));
  }

  @DeleteMapping("/merchants/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  ResponseEntity<Void> archiveMerchant(@PathVariable("id") String id, Authentication auth) {
    service.archiveMerchant(id, AuditActor.from(auth));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/merchants/{id}/verify-binding")
  @PreAuthorize("hasRole('ADMIN')")
  VerifyBindingResponse verifyMerchantBinding(@PathVariable("id") String id, Authentication auth) {
    return service.verifyMerchantBinding(id, AuditActor.from(auth));
  }

  @PostMapping("/merchants/{id}/channels")
  @PreAuthorize("hasRole('ADMIN')")
  ResponseEntity<ChannelResponse> createChannel(@PathVariable("id") String id, @RequestBody String body, Authentication auth) {
    var request = read(body, ChannelCreateRequest.class);
    rejectTenant(body);
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createChannel(id, request, AuditActor.from(auth)));
  }

  @GetMapping("/merchants/{id}/channels")
  @PreAuthorize("hasAnyRole('ADMIN','OPS','FINANCE','AUDITOR')")
  ListResponse<ChannelResponse> listChannels(
      @PathVariable("id") String id,
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "limit", required = false) Integer limit,
      @RequestParam(name = "cursor", required = false) String cursor) {
    return service.listChannels(id, status, PageWindow.of(limit, cursor));
  }

  @GetMapping("/channels/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','OPS','FINANCE','AUDITOR')")
  ChannelResponse getChannel(@PathVariable("id") String id) {
    return service.getChannel(id);
  }

  @PatchMapping("/channels/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  ChannelResponse patchChannel(@PathVariable("id") String id, @RequestBody String body, Authentication auth) {
    var request = read(body, ChannelPatchRequest.class);
    rejectTenant(body);
    return service.patchChannel(id, request, AuditActor.from(auth));
  }

  @DeleteMapping("/channels/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  ResponseEntity<Void> archiveChannel(@PathVariable("id") String id, Authentication auth) {
    service.archiveChannel(id, AuditActor.from(auth));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/channels/{id}/verify-binding")
  @PreAuthorize("hasRole('ADMIN')")
  VerifyBindingResponse verifyChannelBinding(@PathVariable("id") String id, Authentication auth) {
    return service.verifyChannelBinding(id, AuditActor.from(auth));
  }

  private <T> T read(String body, Class<T> type) {
    try {
      return objectMapper.readValue(body, type);
    } catch (JsonProcessingException exception) {
      throw AdminProblems.unprocessable(AdminProblems.MASS_ASSIGNMENT, "request body rejected");
    }
  }

  private void rejectTenant(String body) {
    try {
      InputValidator.rejectTenant(objectMapper.readValue(body, MAP_TYPE));
    } catch (JsonProcessingException exception) {
      throw AdminProblems.unprocessable(AdminProblems.MASS_ASSIGNMENT, "request body rejected");
    }
  }
}
