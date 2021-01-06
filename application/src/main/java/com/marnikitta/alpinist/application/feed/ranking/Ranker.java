package com.marnikitta.alpinist.application.feed.ranking;

import com.marnikitta.alpinist.application.feed.EitherLink;
import com.marnikitta.alpinist.model.Link;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Ranker {
  private static final int MAX_SIBLING_ITEMS = 7;
  private static final int MAX_SIBLINGS = 14;

  public List<Link> closedChildren(String seedName, List<Link> links) {
    final Map<String, List<Link>> graph = childGraph(links);
    final List<Link> list = bfsList(seedName, graph);
    Collections.sort(list);
    return list;
  }

  public List<Link> rankedBySeed(String seedName, List<Link> links) {
    final Optional<Link> seedLink = links.stream().filter(l -> l.name().equals(seedName)).findFirst();

    return seedLink.map(l -> l.payload().outlinks()).orElse(Stream.of())
      .flatMap(outlink -> closedChildren(outlink, links).stream())
      .filter(l -> !l.name().equals(seedName))
      .distinct()
      .sorted()
      .limit(50)
      .collect(Collectors.toList());
  }

  public Map<EitherLink, List<Link>> rankedSiblings(String seedName, List<Link> links) {
    final Map<String, List<Link>> graph = childGraph(links);

    final Map<EitherLink, List<Link>> result = new HashMap<>();
    final Set<String> visited = new HashSet<>();
    for (Link l : bfsList(seedName, graph)) {
      final List<Link> potentialItems = graph.getOrDefault(l.name(), List.of());
      if (potentialItems.size() < 3) {
        continue;
      }
      if (visited.contains(l.name())) {
        continue;
      }
      result.put(
        new EitherLink(l),
        potentialItems.subList(0, Math.min(MAX_SIBLING_ITEMS, potentialItems.size()))
      );
      visited.add(l.name());
      if (result.size() >= MAX_SIBLINGS) {
        break;
      }
    }

    return result;
  }

  private List<Link> bfsList(String seedName, Map<String, List<Link>> graph) {
    final List<Link> result = new ArrayList<>();

    final Set<String> visited = new HashSet<>();
    final Queue<Link> bfsQueue = new ArrayDeque<>(graph.getOrDefault(seedName, Collections.emptyList()));
    visited.add(seedName);

    while (!bfsQueue.isEmpty()) {
      final Link next = bfsQueue.poll();
      result.add(next);
      for (Link child : graph.getOrDefault(next.name(), Collections.emptyList())) {
        if (!visited.contains(child.name())) {
          bfsQueue.offer(child);
        }
      }
      visited.add(next.name());
    }

    return result;
  }

  private Map<String, List<Link>> childGraph(List<Link> links) {
    final Map<String, List<Link>> result = new LinkedHashMap<>();

    for (Link l : links) {
      l.payload().outlinks().forEach(o -> result.compute(o, (name, set) -> {
        final List<Link> children;
        children = Objects.requireNonNullElseGet(set, ArrayList::new);
        children.add(l);
        return children;
      }));
    }
    result.values().forEach(Collections::sort);

    return result;
  }
}
