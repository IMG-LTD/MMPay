package com.imgltd.mmpay.merchant;

import com.imgltd.mmpay.credentials.EnvironmentReferenceResolver;
import com.imgltd.mmpay.credentials.ReferenceResolutionException;
import com.imgltd.mmpay.credentials.ReferenceResolutionFailure;

public final class CredentialBindingService {
  private final EnvironmentReferenceResolver resolver;

  public CredentialBindingService(EnvironmentReferenceResolver resolver) {
    this.resolver = resolver;
  }

  CredentialBindingResult bind(String credentialRef) {
    try {
      var resolved = resolver.resolve(credentialRef);
      return new CredentialBindingResult(resolved.uri(), resolved.fingerprint8());
    } catch (ReferenceResolutionException exception) {
      throw AdminProblems.unprocessable(AdminProblems.CREDENTIAL_REF_INVALID, reason(exception.failure()));
    }
  }

  static String reason(ReferenceResolutionFailure failure) {
    return switch (failure) {
      case ENV_UNSET -> "env_unset";
      case KMS_NO_RESOLVER -> "kms_no_resolver";
      case EMPTY_BYTES -> "empty_bytes";
      case SCHEME_INVALID -> "scheme_invalid";
      case VALUE_TOO_LARGE -> "value_too_large";
    };
  }
}
