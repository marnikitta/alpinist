package com.marnikitta.alpinist.application.feed;

import com.marnikitta.alpinist.application.Template;
import com.marnikitta.alpinist.model.Link;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SiblingsRenderer {
  private static final int MAX_TITLE_LENGTH = 55;
  private final Template siblingTemplate = new Template("links/siblings/sibling.html");
  private final Template itemTemplate = new Template("links/siblings/sibling-item.html");

  private final String prefix;

  public SiblingsRenderer(String prefix) {
    this.prefix = prefix;
  }

  public String render(Link link, List<Link> links) {
    final String items = links.stream().map(this::renderItem).collect(Collectors.joining(""));
    return siblingTemplate.render(Map.of(
      "prefix", this.prefix,
      "name", link.name(),
      "title", link.payload().title(),
      "background", SpaceRenderer.linkBackground(link.name()),
      "items", items
    ));
  }

  private String renderItem(Link item) {
    final String title = item.payload().title();

    final String trimmedTitle;
    if (title.length() > MAX_TITLE_LENGTH) {
      trimmedTitle = title.substring(0, MAX_TITLE_LENGTH) + "...";
    } else {
      trimmedTitle = title;
    }

    return itemTemplate.render(Map.of(
      "prefix", this.prefix,
      "name", item.name(),
      "title", trimmedTitle
    ));
  }
}
