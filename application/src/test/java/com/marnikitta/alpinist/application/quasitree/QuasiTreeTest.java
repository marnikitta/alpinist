package com.marnikitta.alpinist.application.quasitree;

import com.marnikitta.alpinist.model.LinkPayload;
import com.marnikitta.alpinist.service.LinkEncoder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class QuasiTreeTest {
  @Test
  public void testFromDiscussion() {
    final LinkPayload payload = new LinkEncoder().decode(treeBody());
    final QuasiTree tree = QuasiTree.fromLink(payload);

    final Set<String> mlChild = tree.child("ml").collect(Collectors.toSet());
    final Set<String> expected = Set.of("bayesian", "reinforcement", "bandits");
    Assert.assertEquals(mlChild, expected);

    final List<String> banditsParents = tree.parents("bandits").collect(Collectors.toList());
    final List<String> expectedParents = List.of("math", "ml", "reinforcement");
    Assert.assertEquals(banditsParents, expectedParents);
  }

  private String treeBody() {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(
      QuasiTreeTest.class.getClassLoader().getResourceAsStream("quasitree.md"))
    )) {
      return br.lines().collect(Collectors.joining("\n"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}