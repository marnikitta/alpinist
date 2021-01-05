package com.marnikitta.alpinist.model;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    final List<Link> list = new ArrayList<>(List.of(link1, link2));
    Collections.sort(list);

    // Newest first
    Assert.assertEquals(list, List.of(link2, link1));
  }

  @Test
  public void testFilify() {
    final Map<String, String> expectedTransformations = Map.of(
      "https://vas3k.ru/blog/2019/#scroll260",
      "vas3k_blog_2019_scroll260",
      "https://twitter.com/libgen_project",
      "twitter_libgen_project",
      "https://www.gwern.net/docs/psychology/2020-levitt.pdf",
      "gwern_docs_psychology_2020_levitt",
      "http://rama-moyano.medium.com/measuring-feature-success-kpi-definition-for-product-managers-c9fc9cf569da",
      "rama_moyano_medium_measuring_feature_success_kpi_definition",
      "https://statweb.stanford.edu/~owen/courses/363/doenotes.pdf",
      "statweb_stanford_edu_owen_courses_363_doenotes"
    );

    expectedTransformations.forEach((url, expected) -> {
      Assert.assertEquals(Link.filefy(url), expected);
    });
  }
}