package com.imgltd.mmpay.merchant;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AdminProblemHandler {
  @ExceptionHandler(AdminProblemException.class)
  ResponseEntity<ProblemDetail> handle(AdminProblemException exception) {
    var detail = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
    detail.setType(java.net.URI.create(exception.type()));
    detail.setTitle(exception.status().getReasonPhrase());
    return ResponseEntity.status(exception.status()).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(detail);
  }
}
