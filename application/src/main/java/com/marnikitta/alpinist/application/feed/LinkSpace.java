package com.marnikitta.alpinist.application.feed;

import com.marnikitta.alpinist.model.Link;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class LinkSpace {
  private final String name;
  private final @Nullable Link spaceLink;
  private final List<Link> incomingLinks;

  public LinkSpace(String name,
                   @Nullable Link spaceLink,
                   List<Link> incomingLinks) {
    this.name = name;
    this.spaceLink = spaceLink;
    this.incomingLinks = incomingLinks;
  }

  public String name() {
    return this.name;
  }

  public Optional<Link> link() {
    return Optional.ofNullable(this.spaceLink);
  }

  public Stream<Link> incomingLinks() {
    return this.incomingLinks.stream();
  }
}

