package com.imgltd.mmpay.iam;

import com.imgltd.mmpay.audit.AuditWriter;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicePrincipalService {
  private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9._-]{2,63}$");
  private static final int SECRET_BYTES = 32;
  private final AuditWriter auditWriter;
  private final JdbcIamRepository repository;
  private final SecureRandom secureRandom = new SecureRandom();

  public ServicePrincipalService(JdbcIamRepository repository, AuditWriter auditWriter) {
    this.repository = repository;
    this.auditWriter = auditWriter;
  }

  @Transactional
  public ServicePrincipalResponse create(ServicePrincipalRequest request) {
    var name = validateName(request.name());
    var role = IamRole.requireUserRole(request.role());
    var clientId = "svc_" + name + "_" + UUID.randomUUID().toString().substring(0, 8);
    var secret = randomSecret();
    var principal =
        new ServicePrincipalRecord(
            UUID.randomUUID().toString(), UUID.randomUUID().toString(), clientId, role, IamFingerprint.sha256(secret));
    repository.createServicePrincipal(principal);
    auditWriter.emit("user", null, "iam.service-principal.create", "service", clientId, Map.of("role", role));
    return new ServicePrincipalResponse(clientId, secret);
  }

  private String validateName(String name) {
    if (name == null || !NAME_PATTERN.matcher(name).matches()) {
      throw new IllegalArgumentException("service_principal_name_invalid");
    }
    return name;
  }

  private String randomSecret() {
    var bytes = new byte[SECRET_BYTES];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
