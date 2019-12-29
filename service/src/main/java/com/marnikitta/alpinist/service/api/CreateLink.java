package com.marnikitta.alpinist.service.api;

import com.marnikitta.alpinist.model.LinkPayload;

public class CreateLink {
  private final String name;
  private final LinkPayload payload;

  public CreateLink(String name, LinkPayload payload) {
    this.name = name;
    this.payload = payload;
  }

  public String name() {
    return name;
  }

  public LinkPayload payload() {
    return payload;
  }
}
