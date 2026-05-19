package com.imgltd.mmpay.merchant;

import org.springframework.http.HttpStatus;

public class AdminProblemException extends RuntimeException {
  private final String type;
  private final HttpStatus status;

  public AdminProblemException(String type, HttpStatus status, String detail) {
    super(detail);
    this.type = type;
    this.status = status;
  }

  public String type() {
    return type;
  }

  public HttpStatus status() {
    return status;
  }
}
