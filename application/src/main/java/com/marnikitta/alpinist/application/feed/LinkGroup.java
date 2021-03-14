package com.marnikitta.alpinist.application.feed;

import com.marnikitta.alpinist.model.Link;

import java.util.List;
import java.util.stream.Stream;

public class LinkGroup {
  private final String id;
  private final String title;

  private final List<Link> links;

  public LinkGroup(String id, String title, List<Link> links) {
    this.id = id;
    this.title = title;
    this.links = links;
  }

  public String id() {
    return this.id;
  }

  public String title() {
    return this.title;
  }

  public Stream<Link> links() {
    return this.links.stream();
  }
}
