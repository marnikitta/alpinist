package com.marnikitta.alpinist.application.frontend.render;

import com.marnikitta.alpinist.model.Link;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class CachedLinkRenderer implements LinkRenderer {
  private final Map<String, CacheEntry> withActions = new HashMap<>();
  private final Map<String, CacheEntry> withoutActions = new HashMap<>();
  private final LinkRenderer inner;

  public CachedLinkRenderer(LinkRenderer inner) {
    this.inner = inner;
  }

  @Override
  public String renderWithoutActions(Link link) {
    final CacheEntry cacheEntry = withoutActions.get(link.name());
    if (cacheEntry == null || cacheEntry.updated().compareTo(link.updated()) < 0) {
      withoutActions.remove(link.name());
      final String s = inner.renderWithoutActions(link);
      withoutActions.put(link.name(), new CacheEntry(link.updated(), s));
    }

    return withoutActions.get(link.name()).render();
  }

  @Override
  public String renderWithActions(Link link) {
    final CacheEntry cacheEntry = withActions.get(link.name());
    if (cacheEntry == null || cacheEntry.updated().compareTo(link.updated()) < 0) {
      withActions.remove(link.name());
      final String s = inner.renderWithActions(link);
      withActions.put(link.name(), new CacheEntry(link.updated(), s));
    }

    return withActions.get(link.name()).render();
  }

  @Override
  public String renderEdit(Link link) {
    return inner.renderEdit(link);
  }

  private static class CacheEntry {
    private final Instant updated;
    private final String render;

    private CacheEntry(Instant updated, String render) {
      this.updated = updated;
      this.render = render;
    }

    public Instant updated() {
      return updated;
    }

    public String render() {
      return render;
    }
  }
}