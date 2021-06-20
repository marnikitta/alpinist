package com.marnikitta.alpinist.application.feed.ranking.tags;

import com.marnikitta.alpinist.model.Link;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LinkTagItem {
  public final Link link;
  public final String tag;

  private Map<String, Double> features;

  public LinkTagItem(Link link, String tag) {
    this.link = link;
    this.tag = tag;
  }

  public final void setFeature(String name, double value) {
    if (features.containsKey(name)) {
      throw new IllegalArgumentException("Feature " + name + " already set");
    }
    this.features.putIfAbsent(name, value);
  }

  public final double[] toFeaturesArray(List<String> requiredFeatures, double missingValue) {
    final double[] result = new double[requiredFeatures.size()];
    Arrays.fill(result, missingValue);

    for (int i = 0; i < requiredFeatures.size(); ++i) {
      if (features.containsKey(requiredFeatures.get(i))) {
        result[i] = features.get(requiredFeatures.get(i));
      }
    }

    return result;
  }
}
