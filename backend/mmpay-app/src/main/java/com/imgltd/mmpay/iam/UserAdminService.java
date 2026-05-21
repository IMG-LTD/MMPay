package com.imgltd.mmpay.iam;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserAdminService {
  private static final Pattern USERNAME = Pattern.compile("^[a-z][a-z0-9._-]{2,63}$");
  private static final int PASSWORD_MIN = 8;
  private static final int PASSWORD_MAX = 128;

  private final JdbcIamRepository repository;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public UserAdminService(JdbcIamRepository repository) {
    this.repository = repository;
  }

  public List<UserResponse> listUsers() {
    return repository.listHumanUsers().stream()
        .map(u -> new UserResponse(u.username(), u.kind(), u.role(), u.createdAt()))
        .toList();
  }

  @Transactional
  public UserResponse createUser(UserCreateRequest request) {
    validateUsername(request.username());
    validatePassword(request.password());
    var role = requireRole(request.role());
    if (repository.findUser(request.username()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "username_taken");
    }
    var id = UUID.randomUUID().toString();
    var hash = passwordEncoder.encode(request.password());
    repository.createHumanUser(id, request.username(), hash, role);
    return repository.findUser(request.username())
        .map(u -> new UserResponse(u.username(), u.kind(), u.role(), u.createdAt()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "user_create_failed"));
  }

  @Transactional
  public UserResponse patchUser(String username, UserPatchRequest request, String requestingUser) {
    if (repository.findUser(username).filter(u -> "user".equals(u.kind())).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user_not_found");
    }
    if (request.role() != null) {
      if (username.equals(requestingUser)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot_change_own_role");
      }
      var role = requireRole(request.role());
      repository.updateUserRole(username, role);
    }
    if (request.password() != null) {
      validatePassword(request.password());
      repository.updateUserPassword(username, passwordEncoder.encode(request.password()));
    }
    return repository.findUser(username)
        .map(u -> new UserResponse(u.username(), u.kind(), u.role(), u.createdAt()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user_not_found"));
  }

  @Transactional
  public void deleteUser(String username, String requestingUser) {
    if (username.equals(requestingUser)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot_delete_self");
    }
    if (!repository.deleteHumanUser(username)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user_not_found");
    }
  }

  private void validateUsername(String username) {
    if (username == null || !USERNAME.matcher(username).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username_invalid");
    }
  }

  private void validatePassword(String password) {
    if (password == null || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password_invalid");
    }
  }

  private String requireRole(String role) {
    try {
      return IamRole.requireUserRole(role);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "role_invalid");
    }
  }
}
