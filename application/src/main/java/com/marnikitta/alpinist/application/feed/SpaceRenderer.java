package com.marnikitta.alpinist.application.feed;

import com.marnikitta.alpinist.application.Template;
import com.marnikitta.alpinist.model.LinkPayload;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class SpaceRenderer {
  private final static int GRADIENTS_COUNT = 25;
  private final String prefix;
  private final SiblingsRenderer siblingsRenderer;
  private final LinkGroupRenderer linkGroupRenderer;
  private final LinkRenderer linkRenderer;
  private final static MarkdownRenderer renderer = new MarkdownRenderer();

  private final Template spaceTemplate = new Template("links/space.html");

  public SpaceRenderer(String prefix) {
    this.prefix = prefix;
    this.linkGroupRenderer = new LinkGroupRenderer(prefix);
    this.linkRenderer = new LinkRenderer(prefix);
    this.siblingsRenderer = new SiblingsRenderer(prefix);
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

    final StringBuilder linkFeed = new StringBuilder();
    if (space.linkGroups().count() > 1) {
      space.linkGroups()
        .forEach(group -> linkFeed.append(linkGroupRenderer.render(group)));
    } else {
      space.linkGroups().flatMap(LinkGroup::links)
        .forEach(link -> linkFeed.append(linkRenderer.render(link)));
    }
    vars.put("links", linkFeed.toString());

    final StringBuilder siblingsString = new StringBuilder();
    space.siblings().forEach((link, links) -> siblingsString.append(siblingsRenderer.render(link, links)));
    vars.put("siblings", siblingsString.toString());

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
    final int dayOfTheYear = LocalDate.now().getDayOfYear();
    final int gradId = Math.abs(name.hashCode() + dayOfTheYear) % SpaceRenderer.GRADIENTS_COUNT;
    //final int gradId = ThreadLocalRandom.current().nextInt(SpaceRenderer.GRADIENTS_COUNT);
    return "background_gradient_" + gradId;
  }
}
