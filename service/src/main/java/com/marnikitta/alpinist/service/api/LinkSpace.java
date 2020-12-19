package com.marnikitta.alpinist.service.api;

import com.marnikitta.alpinist.model.Link;
import com.sun.istack.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class LinkSpace {
  private final String name;

  @Nullable
  private final Link link;
  private final List<Link> incomingLinks;

  public LinkSpace(Link link, List<Link> incomingLinks) {
    this.name = link.name();
    this.link = link;
    this.incomingLinks = incomingLinks;
  }

  public LinkSpace(String name, List<Link> incomingLinks) {
    this.name = name;
    this.link = null;
    this.incomingLinks = incomingLinks;
  }

  public String name() {
    return this.name;
  }

  public Optional<Link> link() {
    return Optional.ofNullable(this.link);
  }

  public List<Link> incomingLinks() {
    return this.incomingLinks;
  }

  public int incomingCount() {
    return this.incomingLinks.size();
  }

  public Optional<LocalDateTime> lastUpdate() {
    return this.incomingLinks.stream().flatMap(l -> l.payload().updated().stream())
      .min(LocalDateTime::compareTo);
  }
}
