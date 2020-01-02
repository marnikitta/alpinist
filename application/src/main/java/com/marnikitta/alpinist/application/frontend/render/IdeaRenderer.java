package com.marnikitta.alpinist.application.frontend.render;

import com.marnikitta.alpinist.application.IdeaService;
import com.marnikitta.alpinist.application.frontend.Template;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IdeaRenderer {
  private final Template ideaTemplate = new Template("ideas/idea.html");
  private final Template ideasListTemplate = new Template("ideas/ideas.html");

  private final String prefix;

  public IdeaRenderer(String prefix) {
    this.prefix = prefix;
  }

  public String renderIdeas(List<IdeaService.Idea> ideas) {
    final String renderedIdeas = ideas.stream().map(this::renderIdea).collect(Collectors.joining(" "));
    return ideasListTemplate.render("ideas", renderedIdeas);
  }

  private String renderIdea(IdeaService.Idea idea) {
    final Map<String, String> params = new HashMap<>();
    params.put("prefix", prefix);
    params.put("name", idea.sourceName);
    params.put("text", idea.text);
    return ideaTemplate.render(params);
  }
}
