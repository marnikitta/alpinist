package com.marnikitta.alpinist.ml;

import java.util.List;

public interface Model {
  double apply(double[] features);

  List<String> requiredFeatures();
}
