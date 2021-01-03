package com.marnikitta.alpinist.application.feed.ranking;

import com.marnikitta.alpinist.model.Link;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public class Ranker {
  public List<Link> rankBySeed(String seedName, List<Link> links) {
    final Map<String, Set<Link>> graph = childGraph(links);
    return bfsList(seedName, graph);
  }

  private List<Link> bfsList(String seedName, Map<String, Set<Link>> graph) {
    final List<Link> result = new ArrayList<>();

    final Set<String> visited = new HashSet<>();
    final Queue<Link> bfsQueue = new ArrayDeque<>(graph.getOrDefault(seedName, Collections.emptySet()));
    visited.add(seedName);

    while (!bfsQueue.isEmpty()) {
      final Link next = bfsQueue.poll();
      result.add(next);
      for (Link child : graph.getOrDefault(next.name(), Collections.emptySet())) {
        if (!visited.contains(child.name())) {
          bfsQueue.offer(child);
        }
      }
      visited.add(next.name());
    }

    return result;
  }

  private Map<String, Set<Link>> childGraph(Collection<Link> links) {
    final Map<String, Set<Link>> result = new HashMap<>();

    for (Link l : links) {
      l.payload().outlinks().forEach(o -> result.compute(o, (name, set) -> {
        final Set<Link> children;
        children = Objects.requireNonNullElseGet(set, HashSet::new);
        children.add(l);
        return children;
      }));
    }
    return result;
  }
}
