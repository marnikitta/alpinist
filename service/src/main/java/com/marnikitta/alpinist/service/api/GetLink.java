package com.marnikitta.alpinist.service.api;

import java.util.Objects;

public class GetLink {
  private final String name;

  public GetLink(String name) {
    this.name = name;
  }

  public String name() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final GetLink getLink = (GetLink) o;
    return Objects.equals(name, getLink.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
