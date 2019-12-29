package com.marnikitta.alpinist.application.frontend.render;

import com.marnikitta.alpinist.model.LinkGenerator;
import org.testng.annotations.Test;

import java.util.stream.Stream;


public class TemplateLinkRendererTest {
  private final TemplateLinkRenderer renderer = new TemplateLinkRenderer("some_prefix");
  private final LinkGenerator generator = new LinkGenerator();

  @Test
  public void testRenderWithoutActions() {
    Stream.generate(generator::getLink).limit(100).forEach(renderer::renderWithoutActions);
  }

  @Test
  public void testRenderWithActions() {
    Stream.generate(generator::getLink).limit(100).forEach(renderer::renderWithActions);
  }

  @Test
  public void testRenderEdit() {
    Stream.generate(generator::getLink).limit(100).forEach(renderer::renderEdit);
  }
}