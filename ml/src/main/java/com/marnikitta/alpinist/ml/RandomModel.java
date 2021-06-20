package com.marnikitta.alpinist.ml;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RandomModel implements Model {
  @Override
  public double apply(double[] features) {
    return ThreadLocalRandom.current().nextDouble();
  }

  @Override
  public List<String> requiredFeatures() {
    return List.of();
  }
}
