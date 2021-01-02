package com.marnikitta.alpinist;

import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.model.LinkPayload;
import com.marnikitta.alpinist.model.LinkRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

public class InMemoryLinkRepository implements LinkRepository {
  private final Map<String, Link> map = new HashMap<>();
  @Override
  public void sync() {
  }

  @Override
  public Stream<Link> links() {
    return map.values().stream();
  }

  @Override
  public Link create(String name, LinkPayload payload) {
    if (name.isEmpty() || name.isBlank()) {
      throw new IllegalArgumentException("Name shouldn't be empty");
    }
    if (map.containsKey(name)) {
      throw new IllegalArgumentException("Link with name '" + name + "' already exists");
    }

    return map.computeIfAbsent(name, key -> new Link(key, payload));
  }

  @Override
  public boolean delete(String name) {
    return map.remove(name) != null;
  }

  @Override
  public Link update(String name, LinkPayload payload) {
    if (!map.containsKey(name)) {
      throw new NoSuchElementException("Link with name '" + name + "' doesn't exists");
    }
    return map.computeIfPresent(name, (key, link) -> new Link(key, payload));
  }
}
