package com.imgltd.mmpay.merchant;

record PageWindow(int limit, PageCursor cursor) {
  private static final int DEFAULT_LIMIT = 50;
  private static final int MAX_LIMIT = 200;

  static PageWindow of(Integer limit, String cursor) {
    return new PageWindow(normalizeLimit(limit), PageCursor.decode(cursor));
  }

  private static int normalizeLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }
    if (limit < 1 || limit > MAX_LIMIT) {
      throw AdminProblems.unprocessable(AdminProblems.MASS_ASSIGNMENT, "limit out of range");
    }
    return limit;
  }
}
