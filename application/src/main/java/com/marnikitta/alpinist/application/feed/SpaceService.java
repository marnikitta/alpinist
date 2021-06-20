package com.marnikitta.alpinist.application.feed;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import com.marnikitta.alpinist.application.AlpinistFrontend;
import com.marnikitta.alpinist.application.feed.ranking.LinksRanker;
import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.service.api.GetIncomming;
import com.marnikitta.alpinist.service.api.GetLink;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class SpaceService {
  private final SpaceRenderer spaceRenderer = new SpaceRenderer(AlpinistFrontend.PREFIX);
  private static final Duration TIMEOUT_MILLIS = Duration.ofMillis(10000);
  private final ActorRef linkService;
  private final LinksRanker linksRanker = new LinksRanker();

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

    //final List<Link> relevantLinks = ranker.rankedBySeed(name, allL);

    return link
      .thenCombine(allLinks, (l, allL) -> buildSpace(name, l.orElse(null), allL))
      .exceptionally(t -> new LinkSpace(name));
  }

  private LinkSpace buildSpace(String name, @Nullable Link link, List<Link> allL) {
    final List<LinkGroup> resultLinks;
    if (name.equals("recent")) {
      resultLinks = packedIntoDateGroups(allL);
    } else {
      final List<Link> children = linksRanker.closedChildren(name, allL);
      if (!children.isEmpty()) {
        resultLinks = List.of(new LinkGroup("incoming", "Входящие ссылки", children));
      } else {
        resultLinks = Collections.emptyList();
      }
    }

    return new LinkSpace(name, link, resultLinks, linksRanker.rankedSiblings(name, allL));
  }

  private List<LinkGroup> packedIntoDateGroups(List<Link> links) {
    final List<LinkGroup> result = new ArrayList<>();

    final Map<LocalDate, List<Link>> grouped = links.stream()
      .collect(Collectors.groupingBy(
        l -> l.payload().updated().toLocalDate().withDayOfMonth(1),
        TreeMap::new,
        Collectors.mapping(l -> l, Collectors.toList())
      ));
    grouped.forEach((d, l) -> {
      final String standaloneMonth = d.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, new Locale("ru"));
      final String title =
        standaloneMonth.substring(0, 1).toUpperCase() + standaloneMonth.substring(1) + " " + d.getYear();

      result.add(new LinkGroup(d.getYear() + "-" + d.getMonth().toString(), title, l));
    });

    Collections.reverse(result);

    return result;
  }

  public CompletionStage<String> renderedSpace(String name) {
    return fetchSpace(name).thenApply(spaceRenderer::render);
  }
}
