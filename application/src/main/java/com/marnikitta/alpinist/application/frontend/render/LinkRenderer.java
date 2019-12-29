package com.marnikitta.alpinist.application.frontend.render;

import com.marnikitta.alpinist.model.Link;

public interface LinkRenderer {
  String renderWithoutActions(Link link);

  String renderWithActions(Link link);

  String renderEdit(Link link);
}
