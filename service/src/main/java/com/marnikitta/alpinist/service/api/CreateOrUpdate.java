package com.marnikitta.alpinist.service.api;

import com.marnikitta.alpinist.model.Link;

public class CreateOrUpdate {
  public final String name;
  public final Link link;

  public CreateOrUpdate(String name, Link link) {
    this.name = name;
    this.link = link;
  }
}
