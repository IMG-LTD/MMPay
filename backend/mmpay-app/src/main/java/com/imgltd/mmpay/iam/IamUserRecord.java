package com.imgltd.mmpay.iam;

public record IamUserRecord(String username, String kind, String passwordHash, String role, String createdAt) {}

