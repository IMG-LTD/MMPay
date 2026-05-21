package com.imgltd.mmpay.iam;

import com.imgltd.mmpay.audit.AuditWriter;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {
  private final UserAdminService service;
  private final AuditWriter auditWriter;

  public UserAdminController(UserAdminService service, AuditWriter auditWriter) {
    this.service = service;
    this.auditWriter = auditWriter;
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<UserResponse> listUsers() {
    return service.listUsers();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse createUser(@RequestBody UserCreateRequest request, Authentication auth) {
    var user = service.createUser(request);
    auditWriter.emit("user", auth.getName(), "iam.user.create", "user", user.username(),
        Map.of("role", user.role()));
    return user;
  }

  @PatchMapping("/{username}")
  @PreAuthorize("hasRole('ADMIN')")
  public UserResponse patchUser(
      @PathVariable("username") String username,
      @RequestBody UserPatchRequest request,
      Authentication auth) {
    var user = service.patchUser(username, request);
    auditWriter.emit("user", auth.getName(), "iam.user.patch", "user", username,
        Map.of("fields", request.fields()));
    return user;
  }

  @DeleteMapping("/{username}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteUser(@PathVariable("username") String username, Authentication auth) {
    service.deleteUser(username, auth.getName());
    auditWriter.emit("user", auth.getName(), "iam.user.delete", "user", username, Map.of());
  }
}
