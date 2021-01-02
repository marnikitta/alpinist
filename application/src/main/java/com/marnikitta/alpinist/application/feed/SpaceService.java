package com.marnikitta.alpinist.application.feed;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import com.marnikitta.alpinist.application.AlpinistFrontend;
import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.service.api.GetIncomming;
import com.marnikitta.alpinist.service.api.GetLink;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class SpaceService {
  private final SpaceRenderer spaceRenderer = new SpaceRenderer(AlpinistFrontend.PREFIX);
  private static final Duration TIMEOUT_MILLIS = Duration.ofMillis(10000);
  private final ActorRef linkService;

  public SpaceService(ActorRef linkService) {
    this.linkService = linkService;
  }

  public CompletionStage<LinkSpace> fetchSpace(String name) {
    final CompletionStage<Optional<Link>> s = Patterns.ask(linkService, new GetLink(name), TIMEOUT_MILLIS)
      .thenApply(l -> (Optional<Link>) l);

    final CompletionStage<List<Link>> incomingLinks = Patterns.ask(linkService, new GetIncomming(name), TIMEOUT_MILLIS)
      .thenApply(l -> (List<Link>) l);

    return s.thenCombine(
      incomingLinks,
      (link, incoming) -> new LinkSpace(name, link.orElse(null), incoming)
    ).exceptionally(t -> new LinkSpace(name, null, List.of()));
  }

  public CompletionStage<String> renderedSpace(String name) {
    return fetchSpace(name).thenApply(spaceRenderer::render);
  }
}
