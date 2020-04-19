package com.marnikitta.alpinist.application.frontend.render;

import com.marnikitta.alpinist.application.frontend.AlpinistFrontend;

import java.util.HashMap;
import java.util.Map;

public class SpaceRenderer {
  private final Template spaceTemplate = new Template("links/space.html");
  private final TemplateLinkRenderer linkRenderer;

  public SpaceRenderer(String prefix) {
    this.linkRenderer = new TemplateLinkRenderer(prefix);
  }

  public String render(String spaceName, String spaceDiscussion) {
    final Map<String, String> vars = new HashMap<>();
    vars.put("actions", linkRenderer.renderAction(spaceName, "Edit", AlpinistFrontend.LinkAction.EDIT, false));
    vars.put("discussion", linkRenderer.renderMd(spaceDiscussion));

    return spaceTemplate.render(vars);
  }
}
