package com.marnikitta.alpinist.service.api;

import com.marnikitta.alpinist.model.LinkPayload;

public class UpdatePayload {
  private final String name;
  private final LinkPayload newPayload;

  public UpdatePayload(String name, LinkPayload newPayload) {
    this.name = name;
    this.newPayload = newPayload;
  }

  public String name() {
    return name;
  }

  public LinkPayload newPayload() {
    return newPayload;
  }
}
