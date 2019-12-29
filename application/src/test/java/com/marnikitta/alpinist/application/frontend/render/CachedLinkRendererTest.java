package com.marnikitta.alpinist.application.frontend.render;

import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.model.LinkGenerator;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class CachedLinkRendererTest {
  private final TemplateLinkRenderer renderer = new TemplateLinkRenderer("prefix");
  private final CachedLinkRenderer cachedLinkRenderer = new CachedLinkRenderer(renderer);
  private final LinkGenerator generator = new LinkGenerator();

  @Test(invocationCount = 100)
  public void testRenderWithActions() {
    final Link link = generator.getLink();
    assertEquals(renderer.renderWithActions(link), cachedLinkRenderer.renderWithActions(link));
    assertEquals(renderer.renderWithActions(link), cachedLinkRenderer.renderWithActions(link));
    assertEquals(renderer.renderWithActions(link), cachedLinkRenderer.renderWithActions(link));
    final Link updatedLink = generator.updatedLink(link);
    assertEquals(renderer.renderWithActions(updatedLink), cachedLinkRenderer.renderWithActions(updatedLink));
    assertEquals(renderer.renderWithActions(updatedLink), cachedLinkRenderer.renderWithActions(updatedLink));
    assertEquals(renderer.renderWithActions(updatedLink), cachedLinkRenderer.renderWithActions(updatedLink));
  }
}