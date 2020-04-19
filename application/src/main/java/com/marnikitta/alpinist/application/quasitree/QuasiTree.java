package com.marnikitta.alpinist.application.quasitree;

import com.marnikitta.alpinist.model.LinkPayload;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QuasiTree {
  private final Map<String, List<String>> tree = new HashMap<>();
  private final Map<String, String> reversedTree = new HashMap<>();

  public void addEdge(String child, String parent) {
    if (tree.containsKey(child)) {
      return;
    }
    final List<String> children = tree.getOrDefault(parent, new ArrayList<>());
    children.add(child);
    tree.put(parent, children);
    reversedTree.put(child, parent);
  }

  public Optional<String> parent(String child) {
    return Optional.ofNullable(reversedTree.getOrDefault(child, null));
  }

  public Stream<String> parents(String child) {
    final List<String> result = new ArrayList<>();

    String currentVertex = child;
    while (reversedTree.containsKey(currentVertex)) {
      final String parent = reversedTree.get(currentVertex);
      result.add(parent);
      currentVertex = parent;
    }

    Collections.reverse(result);
    return result.stream();
  }

  public Stream<String> child(String parent) {
    final List<String> result = new ArrayList<>();
    final Queue<String> bfs = new ArrayDeque<>();
    bfs.add(parent);

    while (!bfs.isEmpty()) {
      final String next = bfs.remove();
      final List<String> children = tree.getOrDefault(next, List.of());

      result.addAll(children);
      bfs.addAll(children);
    }

    return result.stream();
  }

  public static QuasiTree fromLink(LinkPayload linkPayload) {
    if (linkPayload.tags().noneMatch(t -> t.equals("quasitree"))) {
      return new QuasiTree();
    }

    return fromDiscussion(linkPayload.discussion());
  }

  public static QuasiTree fromDiscussion(String discussion) {
    final QuasiTree result = new QuasiTree();
    final Pattern compile = Pattern.compile("- (?<parent>\\p{Print}+): (?<child>\\p{Print}+)");

    final List<String> lines = discussion
      .lines()
      .filter(l -> l.startsWith("- "))
      .collect(Collectors.toList());

    for (String line : lines) {
      final Matcher matcher = compile.matcher(line);
      if (matcher.matches()) {
        result.addEdge(matcher.group("child"), matcher.group("parent"));
      }
    }

    return result;
  }
}
