package com.marnikitta.alpinist.model;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Set;
import java.util.stream.Collectors;

public class OutlinksTest {
  @Test
  public void testOutlinksMatch() {
    final LinkPayload payload = new LinkPayload(
      "a",
      "",
      "[[outlink1]], [[outlink2]], [[ABA]], [[o_1]], [[123]], [1234]], [[[, ]]]]",
      null
    );
    Assert.assertEquals(Set.of("outlink1", "outlink2", "o_1", "123"), payload.outlinks().collect(Collectors.toSet()));
  }

  @Test
  public void testRendering() {
    final LinkPayload payload = new LinkPayload("a", "", "[[outlink1]], [[outlink2]]", null);
    final String renderedPayload = payload.renderedDiscussion("alpinist.com/");
    Assert.assertEquals(
      renderedPayload,
      "[[[outlink1]]](alpinist.com/outlink1), [[[outlink2]]](alpinist.com/outlink2)"
    );
  }

  @Test
  public void testRussianLinks() {
    final String outlink = "школа_дизайнеров_бюро_горбунова";
    final LinkPayload payload = new LinkPayload("a", "", "[[" + outlink + "]]");
    final String renderedPayload = payload.renderedDiscussion("alpinist.com/");
    Assert.assertEquals(
      renderedPayload,
      "[[[" + outlink + "]]](alpinist.com/" + outlink + ")"
    );
  }
}