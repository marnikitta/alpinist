package com.marnikitta.alpinist.model;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDateTime;

public class LinkTest {

  @Test
  public void testCompareTo() {
    final Link link1 = new Link(
      "test",
      new LinkPayload("title", "", "discussion", LocalDateTime.of(2020, 1, 1, 1, 1))
    );

    final Link link2 = new Link(
      "test",
      new LinkPayload("title", "", "discussion", LocalDateTime.of(2020, 2, 1, 1, 1))
    );

    Assert.assertTrue(link1.compareTo(link2) > 0);
  }
}