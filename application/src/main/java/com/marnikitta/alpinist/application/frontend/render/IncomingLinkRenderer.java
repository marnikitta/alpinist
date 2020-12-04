package com.marnikitta.alpinist.application.frontend.render;

import com.marnikitta.alpinist.model.Link;

import java.util.HashMap;
import java.util.Map;

public class IncomingLinkRenderer {
  private final Template linkTemplate = new Template("links/incoming-link.html");
  private final String prefix;

  public IncomingLinkRenderer(String prefix) {
    this.prefix = prefix;
  }

  public String render(Link link) {
    final Map<String, String> vars = new HashMap<>();
    vars.put("prefix", this.prefix);
    vars.put("name", link.name());
    SpaceRenderer.fillPayloadMap(this.prefix, link.payload(), vars);
    return linkTemplate.render(vars);
  }
}
