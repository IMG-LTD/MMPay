package com.imgltd.mmpay.app.integrations;

import com.imgltd.mmpay.merchant.AdminProblemException;
import org.springframework.http.HttpStatus;

final class IntegrationInput {
  private IntegrationInput() {}

  static String requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw problem("urn:mmpay:problem:request-invalid", name + " must not be blank");
    }
    return value.trim();
  }

  static AdminProblemException problem(String type, String detail) {
    return new AdminProblemException(type, HttpStatus.UNPROCESSABLE_ENTITY, detail);
  }
}
