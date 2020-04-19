package com.marnikitta.alpinist.application.frontend.render;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PopularTagsRenderer {
  private final Template tagTemplate = new Template("tags/tag.html");
  private final Template tagsCloudTemplate = new Template("tags/cloud.html");
  private final String prefix;

  public PopularTagsRenderer(String prefix) {
    this.prefix = prefix;
  }

  public String renderTree(String parentTag, List<String> childTags) {
    final String parent = tagTemplate.render(Map.of("prefix", prefix, "tag", parentTag, "freq", "parent"));
    final String chile = renderInner(childTags, true);
    return tagsCloudTemplate.render(Collections.singletonMap("tags", parent + chile));
  }

  public String render(List<String> tags, boolean sorted) {
    return tagsCloudTemplate.render(Collections.singletonMap("tags", renderInner(tags, sorted)));
  }

  private String renderInner(List<String> tags, boolean sorted) {
    final Map<String, Integer> trans;
    if (sorted) {
      trans = new TreeMap<>();
    } else {
      trans = new LinkedHashMap<>();
    }

    for (int i = 0; i < tags.size(); i++) {
      final int absolute = (tags.size() - i) * 100 / tags.size();
      final int corrected = absolute / 2 + 50;
      final int rounded = corrected / 10 * 10;
      trans.put(tags.get(i), rounded);
    }

    final StringBuilder tagsBody = new StringBuilder();
    trans.forEach((tag, freq) -> {
      final Map<String, String> props = new HashMap<>();
      props.put("prefix", prefix);
      props.put("tag", tag);
      props.put("freq", String.valueOf(freq));
      tagsBody.append(tagTemplate.render(props));
    });
    return tagsBody.toString();
  }
}
