package com.marnikitta.alpinist.application.frontend.render;

import com.marnikitta.alpinist.application.feed.IncomingLinkRenderer;
import com.marnikitta.alpinist.model.LinkGenerator;
import org.testng.annotations.Test;

import java.util.stream.Stream;


public class TemplateLinkRendererTest {
  private final IncomingLinkRenderer renderer = new IncomingLinkRenderer("some_prefix");
  private final LinkGenerator generator = new LinkGenerator();

  @Test
  public void testRender() {
    Stream.generate(generator::getLink).limit(100).forEach(renderer::render);
  }
}