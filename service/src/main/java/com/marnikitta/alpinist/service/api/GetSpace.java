package com.marnikitta.alpinist.service.api;

public class GetSpace {
  private final String name;

  public GetSpace(String name) {
    this.name = name;
  }

  public String name() {
    return name;
  }
}
