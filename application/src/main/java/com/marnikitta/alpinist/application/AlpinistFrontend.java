package com.marnikitta.alpinist.application;

import akka.actor.ActorRef;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.headers.Location;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.pattern.Patterns;
import com.marnikitta.alpinist.application.edit.EditService;
import com.marnikitta.alpinist.application.feed.SpaceService;
import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.model.LinkPayload;
import com.marnikitta.alpinist.service.api.CreateOrUpdate;
import com.marnikitta.alpinist.service.api.Sync;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AlpinistFrontend extends AllDirectives {
  public static final String PREFIX = "";
  public static final Duration TIMEOUT_MILLIS = Duration.ofMillis(10000);

  private final ActorRef linkService;

  private final SpaceService spaceService;
  private final EditService editService;

  private final PageRenderer pageRenderer = new PageRenderer(PREFIX);

  public AlpinistFrontend(ActorRef linkService) {
    this.linkService = linkService;
    this.spaceService = new SpaceService(linkService);
    this.editService = new EditService(linkService);
  }

  public Route route() {
    return encodeResponse(() -> concat(
      pathPrefix("static", () -> getFromDirectory("static")),
      path("sync", () -> post(() -> complete(sync()))),
      pathEndOrSingleSlash(() -> completeWithFuture(renderSpace("recent"))),
      pathPrefix("links", () -> concat(
        pathEndOrSingleSlash(() -> completeWithFuture(renderSpace("recent"))),
        pathPrefix(this::linkRoute)
      ))
    ));
  }

  private Route linkRoute(String name) {
    return concat(
      pathEnd(() -> completeWithFuture(renderSpace(name))),
      path("edit", () -> concat(
        get(() -> completeWithFuture(renderEdit(name))),
        post(() -> formFieldMap(map -> {
          final String title = map.get("title");
          final String nameField = map.get("name");
          final String url = map.get("url");
          final String discussion = map.get("discussion");

          if (title == null || nameField == null || url == null || discussion == null) {
            throw new IllegalArgumentException("Expected discussion and tags params");
          }

          return completeWithFuture(edit(name, nameField, title, url, discussion));
        }))
      ))
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
    return spaceService.renderedSpace(name).thenApply(this::renderBody);
  }

  private CompletionStage<HttpResponse> renderEdit(String name) {
    return editService.renderedEdit(name).thenApply(this::renderBody);
  }

  private HttpResponse sync() {
    linkService.tell(new Sync(), ActorRef.noSender());
    return HttpResponse.create()
      .withStatus(StatusCodes.SEE_OTHER)
      .addHeader(Location.create(PREFIX + '/'));
  }

  private HttpResponse renderBody(String body) {
    return HttpResponse.create()
      .withEntity(ContentTypes.TEXT_HTML_UTF8, pageRenderer.render(body));
  }
}