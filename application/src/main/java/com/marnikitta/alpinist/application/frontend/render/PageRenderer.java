package com.marnikitta.alpinist.application.frontend.render;

import java.util.HashMap;
import java.util.Map;

public class PageRenderer {
  private final String prefix;
  private final Template pageTemplate = new Template("page.html");

  public PageRenderer(String prefix) {
    this.prefix = prefix;
  }

  public String render(String body) {
    final Map<String, String> vars = new HashMap<>();
    vars.put("prefix", prefix);
    vars.put("body", body);
    return pageTemplate.render(vars);
  }
}
