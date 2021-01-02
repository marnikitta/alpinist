package com.marnikitta.alpinist.application.feed;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MarkdownRenderer {
  private static final DataHolder options = new MutableDataSet();
  private static final Parser parser = Parser.builder(options).build();
  private static final HtmlRenderer renderer = HtmlRenderer.builder(options).build();

  private final Map<String, String> renderingCache = new ConcurrentHashMap<>();

  public String renderMarkdown(String md) {
    while (renderingCache.size() > 10000) {
      renderingCache.clear();
    }

    return renderingCache.computeIfAbsent(md, (m) -> {
      final Document document = parser.parse(m);
      return renderer.render(document);
    });
  }
}
