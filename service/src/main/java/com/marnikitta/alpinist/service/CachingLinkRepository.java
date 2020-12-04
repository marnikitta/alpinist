package com.marnikitta.alpinist.service;

import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.model.LinkPayload;
import com.marnikitta.alpinist.model.LinkRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class CachingLinkRepository implements LinkRepository {
  private final LinkRepository inner;
  private final Map<String, Link> cache = new HashMap<>();

  private boolean active = false;

  public CachingLinkRepository(LinkRepository inner) {
    this.inner = inner;
  }

  @Override
  public void sync() {
    inner.sync();
    invalidateAndPopulate();
  }

  @Override
  public Stream<Link> links() {
    if (!active) {
      invalidateAndPopulate();
    }
    return cache.values().stream().sorted();
  }

  @Override
  public Link create(String name, LinkPayload payload) {
    if (!active) {
      invalidateAndPopulate();
    }
    final Link link = inner.create(name, payload);
    cache.put(link.name(), link);
    return link;
  }

  @Override
  public boolean delete(String name) {
    if (!active) {
      invalidateAndPopulate();
    }
    final boolean result = inner.delete(name);
    if (result) {
      cache.remove(name);
    }
    return result;
  }

  @Override
  public Link update(String name, LinkPayload payload) {
    if (!active) {
      invalidateAndPopulate();
    }
    final Link link = inner.update(name, payload);
    cache.put(link.name(), link);
    return link;
  }

  private void invalidateAndPopulate() {
    cache.clear();
    inner.links().forEach(link -> cache.put(link.name(), link));
    active = true;
  }
}
