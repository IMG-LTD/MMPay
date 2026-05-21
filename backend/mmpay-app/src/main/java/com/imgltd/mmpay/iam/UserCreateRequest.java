package com.imgltd.mmpay.iam;

public record UserCreateRequest(String username, String password, String role) {}
