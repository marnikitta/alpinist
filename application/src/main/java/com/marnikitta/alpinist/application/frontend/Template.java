package com.marnikitta.alpinist.application.frontend;

import com.marnikitta.alpinist.application.frontend.render.TemplateLinkRenderer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class Template {
  private final String template;

  public Template(String resourceName) {
    this.template = resource(resourceName);
  }

  public String render(Map<String, String> variables) {
    final String[] result = {template};
    variables.forEach((variable, value) -> {
      result[0] = result[0].replaceAll("\\$\\{" + variable + '}', Matcher.quoteReplacement(value));
    });

    if (result[0].split("\\$\\{[a-zA-Z]+\\}").length > 1) {
      throw new IllegalArgumentException("Not all placeholders are filled");
    }
    return result[0];
  }

  private String resource(String name) {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(
      TemplateLinkRenderer.class.getClassLoader().getResourceAsStream("templates/" + name))
    )) {
      return br.lines().collect(Collectors.joining("\n"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
