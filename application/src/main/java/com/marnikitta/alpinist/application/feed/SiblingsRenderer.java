package com.marnikitta.alpinist.application.feed;

import com.marnikitta.alpinist.application.Template;
import com.marnikitta.alpinist.model.Link;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SiblingsRenderer {
  private final Template siblingTemplate = new Template("links/siblings/sibling.html");
  private final Template itemTemplate = new Template("links/siblings/sibling-item.html");

  private final String prefix;

  public SiblingsRenderer(String prefix) {
    this.prefix = prefix;
  }

  public String render(EitherLink link, List<Link> links) {
    final String items = links.stream().map(this::renderItem).collect(Collectors.joining(""));
    return siblingTemplate.render(Map.of(
      "prefix", this.prefix,
      "name", link.name(),
      "title", link.titleOrName(),
      "background", SpaceRenderer.linkBackground(link.name()),
      "items", items
    ));
  }

  private String renderItem(Link item) {
    return itemTemplate.render(Map.of(
      "prefix", this.prefix,
      "name", item.name(),
      "title", item.payload().title()
    ));
  }
}
