package com.marnikitta.alpinist.application.frontend.render;

import com.marnikitta.alpinist.model.LinkGenerator;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PopularTagsRendererTest {
  private final LinkGenerator generator = new LinkGenerator();
  private final PopularTagsRenderer renderer = new PopularTagsRenderer("prefix");

  @Test(invocationCount = 100)
  public void testRender() {
    final List<String> tags = Stream.generate(generator::nextWord).limit(100).collect(Collectors.toList());
    renderer.render(tags, true);
  }
}