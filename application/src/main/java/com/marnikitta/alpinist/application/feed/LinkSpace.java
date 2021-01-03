package com.marnikitta.alpinist.application.feed;

import com.marnikitta.alpinist.model.Link;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class LinkSpace {
  private final String name;
  private final @Nullable Link spaceLink;
  private final List<Link> incomingLinks;
  private final List<Link> relevantLinks;

  public LinkSpace(String name) {
    this(name, null, Collections.emptyList(), Collections.emptyList());
  }

  public LinkSpace(String name,
                   @Nullable Link spaceLink,
                   List<Link> incomingLinks,
                   List<Link> relevantLinks) {
    this.name = name;
    this.spaceLink = spaceLink;
    this.incomingLinks = incomingLinks;
    this.relevantLinks = relevantLinks;
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

  public Stream<Link> relevantLinks() {
    return this.relevantLinks.stream();
  }
}

