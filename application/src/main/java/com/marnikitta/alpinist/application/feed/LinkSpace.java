package com.marnikitta.alpinist.application.feed;

import com.marnikitta.alpinist.model.Link;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class LinkSpace {
  private final String name;
  private final @Nullable Link spaceLink;

  private final List<LinkGroup> linkGroups;

  private final Map<Link, List<Link>> siblings;

  public LinkSpace(String name) {
    this(
      name,
      null,
      Collections.emptyList(),
      Collections.emptyMap()
    );
  }

  public LinkSpace(String name,
                   @Nullable Link spaceLink,
                   List<LinkGroup> linkGroups,
                   Map<Link, List<Link>> siblings) {
    this.name = name;
    this.spaceLink = spaceLink;
    this.linkGroups = linkGroups;
    this.siblings = siblings;
  }

  public String name() {
    return this.name;
  }

  public Optional<Link> link() {
    return Optional.ofNullable(this.spaceLink);
  }

  public Stream<LinkGroup> linkGroups() {
    return this.linkGroups.stream();
  }

  public Map<Link, List<Link>> siblings() {
    return this.siblings;
  }

}

