package com.marnikitta.alpinist.service.api;

public class GetLink {
  private final String name;

  public GetLink(String name) {
    this.name = name;
  }

  public String name() {
    return name;
  }
}
