package com.marnikitta.alpinist.application.frontend.render;

import com.marnikitta.alpinist.application.frontend.Template;

import java.util.HashMap;
import java.util.Map;

public class PageRenderer {
  private final String prefix;
  private final Template pageTemplate = new Template("page.html");

  public PageRenderer(String prefix) {
    this.prefix = prefix;
  }

  public String render(String body) {
    return render(body, "");
  }

  public String render(String body, String title) {
    final Map<String, String> vars = new HashMap<>();
    vars.put("prefix", prefix);
    vars.put("body", body);
    vars.put("topic", title);
    return pageTemplate.render(vars);
  }
}
