package com.marnikitta.alpinist.application.frontend;

import akka.actor.ActorRef;
import akka.http.javadsl.coding.Coder;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.headers.Location;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.pattern.Patterns;
import com.marnikitta.alpinist.application.frontend.render.EditLinkRenderer;
import com.marnikitta.alpinist.application.frontend.render.PageRenderer;
import com.marnikitta.alpinist.application.frontend.render.SpaceRenderer;
import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.model.LinkPayload;
import com.marnikitta.alpinist.service.api.CreateOrUpdate;
import com.marnikitta.alpinist.service.api.GetLink;
import com.marnikitta.alpinist.service.api.GetSpace;
import com.marnikitta.alpinist.service.api.LinkSpace;
import com.marnikitta.alpinist.service.api.Sync;
import com.marnikitta.alpinist.tg.Alert;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AlpinistFrontend extends AllDirectives {
  private static final String PREFIX = "";
  public static final Duration TIMEOUT_MILLIS = Duration.ofMillis(10000);
  private final ActorRef linkService;

  private final PageRenderer pageRenderer = new PageRenderer(PREFIX);
  private final SpaceRenderer spaceRenderer = new SpaceRenderer(PREFIX);
  private final EditLinkRenderer editLinkRenderer = new EditLinkRenderer(PREFIX);

  public AlpinistFrontend(ActorRef linkService) {
    this.linkService = linkService;
  }

  public Route route() {
    return concat(
      pathPrefix("static", () -> getFromDirectory("static")),
      path("sync", () -> post(() -> complete(sync()))),
      pathEndOrSingleSlash(() -> completeWithFuture(renderSpace("recent"))),
      pathPrefix("links", () -> concat(
        pathEndOrSingleSlash(() -> completeWithFuture(renderSpace("recent"))),
        pathPrefix(this::linkRoute)
      ))
    );
  }

  private Route linkRoute(String name) {
    return concat(
      pathEnd(() -> encodeResponseWith(
        List.of(Coder.Gzip),
        () -> completeWithFuture(renderSpace(name))
      )),
      get(() -> path("edit", () -> completeWithFuture(renderEdit(name))
      )),
      post(() -> path("edit", () -> formFieldMap(map -> {
        final String title = map.get("title");
        final String nameField = map.get("name");
        final String url = map.get("url");
        final String discussion = map.get("discussion");

        if (title == null || nameField == null || url == null || discussion == null) {
          throw new IllegalArgumentException("Expected discussion and tags params");
        }

        return completeWithFuture(edit(name, nameField, title, url, discussion));
      })))
    );
  }

  private CompletionStage<HttpResponse> edit(String originalName,
                                             String name,
                                             String title,
                                             String url,
                                             String discussion) {
    return Patterns.ask(
      linkService,
      new CreateOrUpdate(
        originalName,
        new Link(name, new LinkPayload(title.trim(), url.trim(), discussion.replace("\r\n", "\n").trim()))
      ),
      TIMEOUT_MILLIS
    ).thenCompose(o -> CompletableFuture.completedFuture(HttpResponse.create()
      .withStatus(StatusCodes.SEE_OTHER)
      .addHeader(Location.create(PREFIX + "/links/" + name))
    ));
  }

  private CompletionStage<HttpResponse> renderSpace(String name) {
    return Patterns.ask(linkService, new GetSpace(name), TIMEOUT_MILLIS)
      .thenApply(result -> (LinkSpace) result)
      .exceptionally(t -> new LinkSpace(name, List.of()))
      .thenApply(space -> renderBody(spaceRenderer.render(space)));
  }

  private HttpResponse sync() {
    linkService.tell(new Sync(), ActorRef.noSender());
    return HttpResponse.create()
      .withStatus(StatusCodes.SEE_OTHER)
      .addHeader(Location.create(PREFIX + '/'));
  }

  private CompletionStage<HttpResponse> renderEdit(String name) {
    final CompletionStage<Link> link = Patterns.ask(linkService, new GetLink(name), TIMEOUT_MILLIS)
      .thenApply(response -> (Optional<Link>) response)
      .thenApply(l -> l.orElse(new Link(name, new LinkPayload(name, "", ""))));

    final CompletionStage<List<String>> frequentOutlinks = frequentOutlinks();

    return link.thenCombine(frequentOutlinks, (Link l, List<String> outlinks) -> renderBody(editLinkRenderer.render(
      l,
      outlinks
      ))
    );
  }

  private CompletionStage<List<String>> frequentOutlinks() {
    return Patterns.ask(linkService, new GetSpace("recent"), TIMEOUT_MILLIS)
      .thenApply(s -> (LinkSpace) s)
      .thenApply(s -> {
        final LinkedHashSet<String> result = new LinkedHashSet<>();

        final List<Link> allLinks = s.incomingLinks();

        // 3 most recent links
        allLinks.stream().sorted().limit(3).map(Link::name).forEach(result::add);

        // 40 most recent outlinks
        allLinks.stream()
          .sorted()
          .limit(100)
          .flatMap(l -> l.payload().outlinks())
          .distinct()
          .limit(40)
          .forEach(result::add);

        // 10 most frequent outlinks
        final Map<String, Long> outlinkFrequency = allLinks.stream().flatMap(l -> l.payload().outlinks())
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

  private HttpResponse renderBody(String body) {
    return HttpResponse.create()
      .withEntity(ContentTypes.TEXT_HTML_UTF8, pageRenderer.render(body));
  }
}