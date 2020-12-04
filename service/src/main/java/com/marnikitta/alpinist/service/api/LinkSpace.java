package com.marnikitta.alpinist.service.api;

import com.marnikitta.alpinist.model.Link;
import com.sun.istack.Nullable;

import java.util.List;
import java.util.Optional;

public class LinkSpace {
  private final String name;

  @Nullable
  private final Link link;
  private final List<Link> incommingLinks;

  public LinkSpace(Link link, List<Link> incommingLinks) {
    this.name = link.name();
    this.link = link;
    this.incommingLinks = incommingLinks;
  }

  public LinkSpace(String name, List<Link> incommingLinks) {
    this.name = name;
    this.link = null;
    this.incommingLinks = incommingLinks;
  }

  public String name() {
    return this.name;
  }

  public Optional<Link> link() {
    return Optional.ofNullable(this.link);
  }

  public List<Link> incommingLinks() {
    return this.incommingLinks;
  }
}
