package com.imgltd.mmpay.merchant;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

record PageCursor(long createdAtMicros, String id) {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  static String encode(Instant createdAt, String id) {
    try {
      var micros = Math.multiplyExact(createdAt.getEpochSecond(), 1_000_000L) + createdAt.getNano() / 1_000L;
      var json = MAPPER.writeValueAsString(new CursorJson(micros, id));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    } catch (Exception exception) {
      throw new IllegalStateException("cursor encode failed", exception);
    }
  }

  static PageCursor decode(String encoded) {
    if (encoded == null || encoded.isBlank()) {
      return null;
    }
    try {
      var json = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
      var cursor = MAPPER.readValue(json, CursorJson.class);
      return new PageCursor(cursor.created_at_us(), cursor.id());
    } catch (Exception exception) {
      throw AdminProblems.unprocessable(AdminProblems.MASS_ASSIGNMENT, "cursor invalid");
    }
  }

  Instant createdAt() {
    var seconds = createdAtMicros / 1_000_000L;
    var nanos = (createdAtMicros % 1_000_000L) * 1_000L;
    return Instant.ofEpochSecond(seconds, nanos);
  }

  private record CursorJson(long created_at_us, String id) {}
}
