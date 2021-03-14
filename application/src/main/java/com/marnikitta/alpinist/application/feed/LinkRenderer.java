package com.marnikitta.alpinist.application.feed;

import com.marnikitta.alpinist.application.Template;
import com.marnikitta.alpinist.model.Link;

import java.util.HashMap;
import java.util.Map;

public class LinkRenderer {
  private final Template linkTemplate = new Template("links/link.html");
  private final String prefix;

  public LinkRenderer(String prefix) {
    this.prefix = prefix;
  }

  public String render(Link link) {
    final Map<String, String> vars = new HashMap<>();
    vars.put("prefix", this.prefix);
    vars.put("name", link.name());
    vars.put("inplace-edit-hidden", "hidden");

    // FIXME
    SpaceRenderer.fillPayloadMap(this.prefix, link.payload(), vars);

    vars.put("url", this.prefix + "/links/" + link.name());
    if (!link.payload().url().isEmpty()) {
      vars.put("url", link.payload().url());
    }

    if (link.payload().rawDiscussion().isEmpty()) {
      vars.put("inplace-edit-hidden", "");
    }
    return linkTemplate.render(vars);
  }
}
