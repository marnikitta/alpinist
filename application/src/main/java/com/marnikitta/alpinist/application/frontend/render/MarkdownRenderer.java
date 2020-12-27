package com.marnikitta.alpinist.application.frontend.render;

import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MarkdownRenderer {
  private static final MutableDataSet options = new MutableDataSet();
  private static final Parser parser = Parser.builder(options).build();
  private static final HtmlRenderer renderer = HtmlRenderer.builder(options).build();

  private final Map<String, String> renderingCache = new ConcurrentHashMap<>();

  public String renderMarkdown(String md) {
    while (renderingCache.size() > 10000) {
      renderingCache.clear();
    }

    return renderingCache.computeIfAbsent(md, (m) -> {
      final Node document = parser.parse(m);
      return renderer.render(document);
    });
  }
}
