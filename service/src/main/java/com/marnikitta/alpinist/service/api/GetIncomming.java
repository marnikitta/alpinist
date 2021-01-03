package com.marnikitta.alpinist.service.api;

public class GetIncomming {
  private final String name;

  public GetIncomming(String name) {
    this.name = name;
  }

  public GetIncomming() {
    this.name = "recent";
  }

  public String name() {
    return name;
  }
}
