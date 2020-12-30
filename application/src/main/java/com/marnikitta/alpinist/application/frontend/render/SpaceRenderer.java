package com.marnikitta.alpinist.application.frontend.render;

import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.model.LinkPayload;
import com.marnikitta.alpinist.service.api.LinkSpace;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SpaceRenderer {
  private final static int GRADIENTS_COUNT = 25;
  private final String prefix;
  private final IncomingLinkRenderer incomingLinkRenderer;
  private final static MarkdownRenderer renderer = new MarkdownRenderer();

  private final Template spaceTemplate = new Template("links/space.html");

  public SpaceRenderer(String prefix) {
    this.prefix = prefix;
    this.incomingLinkRenderer = new IncomingLinkRenderer(prefix);
  }

  public String render(LinkSpace space) {
    final Map<String, String> vars = new HashMap<>();
    vars.put("title", space.name());
    vars.put("name", space.name());
    vars.put("prefix", this.prefix);

    vars.put("actions-hidden", "");
    if (space.name().equals("recent")) {
      vars.put("actions-hidden", "hidden");
    }

    vars.put("url", "");
    vars.put("hidden", "hidden");
    vars.put("discussion", "");

    final String background = linkBackground(space.name());
    vars.put("background", background);

    vars.put("discussion-hidden", "hidden");
    if (space.link().isPresent()) {
      final LinkPayload payload = space.link().get().payload();
      fillPayloadMap(this.prefix, payload, vars);

      if (!payload.rawDiscussion().isEmpty()) {
        vars.put("discussion-hidden", "");
      }
    }

    final StringBuilder incomingLinks = new StringBuilder();
    for (Link incomingLink : space.incomingLinks()) {
      incomingLinks.append(incomingLinkRenderer.render(incomingLink));
    }
    vars.put("incoming-links", incomingLinks.toString());

    return spaceTemplate.render(vars);
  }

  public static void fillPayloadMap(String prefix, LinkPayload payload, Map<String, String> vars) {
    vars.put("url", "");
    vars.put("hidden", "hidden");
    vars.put("title", payload.title());

    if (!payload.url().isEmpty()) {
      vars.put("url", payload.url());
      vars.put("hidden", "");
    }

    final String outlinkPrefix = prefix + "/links/";
    final String outlinkPattern = "<a href=\"" + outlinkPrefix + "%s\" class=\"outlink\">[[%s]]</a>";
    vars.put(
      "discussion",
      renderer.renderMarkdown(payload.renderedDiscussion(o -> String.format(outlinkPattern, o, o)))
    );
  }

  public static String linkBackground(String name) {
    //final int gradId = Math.abs(name.hashCode()) % SpaceRenderer.GRADIENTS_COUNT;
    final int gradId = ThreadLocalRandom.current().nextInt(SpaceRenderer.GRADIENTS_COUNT);
    return "background_gradient_" + gradId;
  }
}
