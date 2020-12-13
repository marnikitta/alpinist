package com.marnikitta.alpinist.application.frontend.render;

import com.marnikitta.alpinist.application.frontend.AlpinistFrontend;
import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.model.LinkPayload;
import com.marnikitta.alpinist.service.api.LinkSpace;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;

import java.util.HashMap;
import java.util.Map;

public class SpaceRenderer {
  private final static int GRADIENTS_COUNT = 25;
  private final String prefix;
  private final IncomingLinkRenderer incomingLinkRenderer;

  private final Template spaceTemplate = new Template("links/space.html");
  private final Template actionTemplate = new Template("links/action.html");

  private static final MutableDataSet options = new MutableDataSet();
  private static final Parser parser = Parser.builder(options).build();
  private static final HtmlRenderer renderer = HtmlRenderer.builder(options).build();

  public SpaceRenderer(String prefix) {
    this.prefix = prefix;
    this.incomingLinkRenderer = new IncomingLinkRenderer(prefix);
  }

  public String render(LinkSpace space) {
    final Map<String, String> vars = new HashMap<>();
    vars.put("title", space.name());
    vars.put("actions", renderActions(space.name()));

    if (space.name().equals("recent")) {
      vars.put("actions", "");
    }

    vars.put("url", "");
    vars.put("hidden", "hidden");
    vars.put("discussion", "");

    final String background = linkBackground(space.name());
    vars.put("background", background);

    if (space.link().isPresent()) {
      final LinkPayload payload = space.link().get().payload();
      fillPayloadMap(this.prefix, payload, vars);
    }

    final StringBuilder incommingLinks = new StringBuilder();
    for (Link incommingLink : space.incommingLinks()) {
      incommingLinks.append(incomingLinkRenderer.render(incommingLink));
    }
    vars.put("incoming-links", incommingLinks.toString());

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
    final String outlinkPattern = "<a href=\"" + outlinkPrefix + "%s\" class=\"%s outlink\">%s</a>";
    vars.put("discussion", renderMd(payload.renderedDiscussion(o -> String.format(outlinkPattern, o, linkBackground(o), o))));
  }

  private String renderActions(String linkName) {
    return renderAction(linkName, "Edit", AlpinistFrontend.LinkAction.EDIT, false)
      + renderAction(linkName, "Delete", AlpinistFrontend.LinkAction.DELETE, true);
  }

  public String renderAction(String linkName, String name, AlpinistFrontend.LinkAction action, boolean post) {
    final Map<String, String> vars = new HashMap<>();
    vars.put("name", linkName);
    vars.put("action", action.encoded);
    vars.put("actionName", name);
    vars.put("prefix", prefix);

    if (post) {
      vars.put("method", "post");
    } else {
      vars.put("method", "get");
    }

    return actionTemplate.render(vars);
  }

  public static String renderMd(String md) {
    final Node document = parser.parse(md);
    return renderer.render(document);
  }

  public static String linkBackground(String name) {
    final int gradId = Math.abs(name.hashCode()) % SpaceRenderer.GRADIENTS_COUNT;
    return "background_gradient_" + gradId;
  }
}
