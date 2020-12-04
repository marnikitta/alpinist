package com.marnikitta.alpinist.application.frontend.render;

import com.marnikitta.alpinist.model.Link;

import java.util.Map;

public class EditLinkRenderer {
  private final Template editLinkTemplate = new Template("links/edit.html");
  private final String prefix;

  public EditLinkRenderer(String prefix) {
    this.prefix = prefix;
  }

  public String render(Link link) {
    final Map<String, String> vars = Map.of(
      "name", link.name(),
      "title", link.payload().title(),
      "url", link.payload().url(),
      "discussion", link.payload().rawDiscussion(),
      "prefix", this.prefix
    );

    return editLinkTemplate.render(vars);
  }
}
