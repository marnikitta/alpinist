package com.marnikitta.alpinist.application.feed;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import com.marnikitta.alpinist.application.AlpinistFrontend;
import com.marnikitta.alpinist.application.feed.ranking.Ranker;
import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.service.api.GetIncomming;
import com.marnikitta.alpinist.service.api.GetLink;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class SpaceService {
  private final SpaceRenderer spaceRenderer = new SpaceRenderer(AlpinistFrontend.PREFIX);
  private static final Duration TIMEOUT_MILLIS = Duration.ofMillis(10000);
  private final ActorRef linkService;
  private final Ranker ranker = new Ranker();

  public SpaceService(ActorRef linkService) {
    this.linkService = linkService;
  }

  public CompletionStage<LinkSpace> fetchSpace(String name) {
    final CompletableFuture<Optional<Link>> link = Patterns.ask(
      linkService,
      new GetLink(name),
      TIMEOUT_MILLIS
    ).thenApply(l -> (Optional<Link>) l)
      .toCompletableFuture();

    final CompletableFuture<List<Link>> allLinks = Patterns.ask(
      linkService,
      new GetIncomming(),
      TIMEOUT_MILLIS
    ).thenApply(l -> (List<Link>) l).toCompletableFuture();

    return link.thenCombine(allLinks, (l, allL) -> new LinkSpace(
      name,
      l.orElse(null),
      name.equals("recent") ? allL : ranker.closedChildren(name, allL),
      ranker.rankedBySeed(name, allL),
      ranker.rankedSiblings(name, allL)
    ))
      .exceptionally(t -> new LinkSpace(name));
  }

  public CompletionStage<String> renderedSpace(String name) {
    return fetchSpace(name).thenApply(spaceRenderer::render);
  }
}
