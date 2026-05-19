package com.imgltd.mmpay.setup;

public record SetupRequest(String username, String password, String passwordConfirm, String initialLocale) {}
