package com.imgltd.mmpay.merchant;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ListResponse<T>(
    List<T> items, @JsonProperty("next_cursor") String nextCursor, @JsonProperty("has_more") boolean hasMore) {}
