package com.marnikitta.alpinist.application.edit;

import com.marnikitta.alpinist.application.Template;
import com.marnikitta.alpinist.application.feed.SpaceRenderer;
import com.marnikitta.alpinist.model.Link;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EditLinkRenderer {
  private final Template editLinkTemplate = new Template("links/edit.html");
  private final Template suggestedTagTemplate = new Template("links/suggested_tag.html");
  private final String prefix;

  public EditLinkRenderer(String prefix) {
    this.prefix = prefix;
  }

  public String render(Link link, List<String> suggestedTags) {
    final String tagsHtml = suggestedTags.stream()
      .map(t -> suggestedTagTemplate.render(Map.of("name", t, "background", SpaceRenderer.linkBackground(t))))
      .collect(Collectors.joining());

    final Map<String, String> vars = Map.of(
      "name", link.name(),
      "title", link.payload().title(),
      "url", link.payload().url(),
      "discussion", link.payload().rawDiscussion(),
      "prefix", this.prefix,
      "background", SpaceRenderer.linkBackground(link.name()),
      "suggested-tags", tagsHtml
    );

    return editLinkTemplate.render(vars);
  }
}
