package com.marnikitta.alpinist.application.edit;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import com.marnikitta.alpinist.application.AlpinistFrontend;
import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.model.LinkPayload;
import com.marnikitta.alpinist.service.api.GetIncomming;
import com.marnikitta.alpinist.service.api.GetLink;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EditService {
  private final ActorRef linkService;
  private final EditLinkRenderer editRenderer = new EditLinkRenderer(AlpinistFrontend.PREFIX);

  public EditService(ActorRef linkService) {
    this.linkService = linkService;
  }

  public CompletionStage<List<String>> suggestedTags(String name) {
    return Patterns.ask(linkService, new GetIncomming("recent"), Duration.ofSeconds(10))
      .thenApply(s -> (List<Link>) s)
      .thenApply(s -> {
        final LinkedHashSet<String> result = new LinkedHashSet<>();

        // 40 most recent outlinks
        s.stream()
          .sorted()
          .limit(100)
          .flatMap(l -> l.payload().outlinks())
          .distinct()
          .limit(40)
          .forEach(result::add);

        // 2 most recent links
        s.stream().sorted().limit(3).map(Link::name).forEach(result::add);

        // 10 most frequent outlinks
        final Map<String, Long> outlinkFrequency = s.stream().flatMap(l -> l.payload().outlinks())
          .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        final List<Map.Entry<String, Long>> sortedValues = new ArrayList<>(outlinkFrequency.entrySet());
        sortedValues.sort(Map.Entry.comparingByValue());
        Collections.reverse(sortedValues);
        final List<String> frequentOutlinks = sortedValues.stream().map(Map.Entry::getKey).collect(Collectors.toList());
        result.addAll(frequentOutlinks.subList(0, Math.min(10, frequentOutlinks.size())));

        return (List<String>) new ArrayList<>(result);
      })
      .exceptionally(e -> List.of());
  }

  public CompletionStage<String> renderedEdit(String name) {
    final CompletionStage<Link> link = Patterns.ask(linkService, new GetLink(name), AlpinistFrontend.TIMEOUT_MILLIS)
      .thenApply(response -> (Optional<Link>) response)
      .thenApply(l -> l.orElse(new Link(name, new LinkPayload(name, "", ""))));

    final CompletionStage<List<String>> frequentOutlinks = suggestedTags(name);

    return link.thenCombine(frequentOutlinks, editRenderer::render);
  }
}
