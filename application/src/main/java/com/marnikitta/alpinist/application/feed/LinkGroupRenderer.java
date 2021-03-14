package com.marnikitta.alpinist.application.feed;

import com.marnikitta.alpinist.application.Template;

import java.util.Map;
import java.util.stream.Collectors;

public class LinkGroupRenderer {
  private final Template linkGroupTemplate = new Template("links/link-group.html");
  private final LinkRenderer linkRenderer;

  public LinkGroupRenderer(String prefix) {
    this.linkRenderer = new LinkRenderer(prefix);
  }

  public String render(LinkGroup linkGroup) {
    final String links = linkGroup.links().map(linkRenderer::render).collect(Collectors.joining());

    return linkGroupTemplate.render(Map.of(
      "id", linkGroup.id(),
      "title", linkGroup.title(),
      "links", links
    ));
  }
}
