package com.marnikitta.alpinist.service.api;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

public class GetLinks {
  private final Set<String> tags;
  private final int offset;
  private final int limit;

  public GetLinks(Set<String> tags, int offset, int limit) {
    this.tags = tags;
    this.limit = limit;
    this.offset = offset;
  }

  public GetLinks(String tag) {
    this(Collections.singleton(tag), 0, Integer.MAX_VALUE);
  }

  public GetLinks() {
    this(Collections.emptySet(), 0, Integer.MAX_VALUE);
  }

  public int offset() {
    return offset;
  }

  public int limit() {
    return limit;
  }

  public Stream<String> tags() {
    return tags.stream();
  }
}
