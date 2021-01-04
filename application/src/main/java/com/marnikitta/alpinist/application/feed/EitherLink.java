package com.marnikitta.alpinist.application.feed;

import com.marnikitta.alpinist.model.Link;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class EitherLink {
  private final String name;
  private final @Nullable Link link;

  public EitherLink(String name) {
    this.link = null;
    this.name = name;
  }

  public EitherLink(Link link) {
    this.link = link;
    this.name = link.name();
  }

  public String name() {
    return this.name;
  }

  public String titleOrName() {
    if (link != null) {
      return link.payload().title();
    } else {
      return name;
    }
  }

  public Optional<Link> link() {
    return Optional.ofNullable(link);
  }
}
