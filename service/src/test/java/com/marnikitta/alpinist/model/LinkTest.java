package com.marnikitta.alpinist.model;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LinkTest {

  @Test(invocationCount = 10)
  public void testCompareTo() {
    final Link link1 = new Link(
      "test",
      Instant.ofEpochSecond(1),
      Instant.ofEpochSecond(1),
      new LinkPayload("title", Set.of(CommonTags.UNREAD), "discussion")
    );

    final Link link2 = new Link("test", Instant.ofEpochSecond(1), Instant.ofEpochSecond(2), link1.payload());
    final Link link3 = new Link(
      "test",
      Instant.ofEpochSecond(10),
      Instant.ofEpochSecond(20),
      link1.payload().withRemovedTags(CommonTags.UNREAD)
    );
    final Link link4 = new Link(
      "test",
      Instant.ofEpochSecond(10),
      Instant.ofEpochSecond(20),
      link1.payload().withRemovedTags(CommonTags.UNREAD).withAddedTags(CommonTags.SHELVED)
    );

    final List<Link> links = new ArrayList<>(List.of(link1, link2, link3, link4));
    Collections.shuffle(links);

    Collections.sort(links);

    Assert.assertEquals(links, List.of(link2, link1, link4, link3));
  }
}