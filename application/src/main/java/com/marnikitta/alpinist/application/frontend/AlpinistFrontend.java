package com.marnikitta.alpinist.application.frontend;

import akka.actor.ActorRef;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpCharsets;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.MediaTypes;
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
import com.marnikitta.alpinist.service.api.DeleteLink;
import com.marnikitta.alpinist.service.api.GetLink;
import com.marnikitta.alpinist.service.api.GetSpace;
import com.marnikitta.alpinist.service.api.LinkSpace;
import com.marnikitta.alpinist.service.api.Sync;
import com.marnikitta.alpinist.tg.Alert;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AlpinistFrontend extends AllDirectives {
  private static final String PREFIX = "";
  public static final Duration TIMEOUT_MILLIS = Duration.ofMillis(10000);
  private final ActorRef linkService;
  private final ActorRef tgService;

  private final PageRenderer pageRenderer = new PageRenderer(PREFIX);
  private final SpaceRenderer spaceRenderer = new SpaceRenderer(PREFIX);
  private final EditLinkRenderer editLinkRenderer = new EditLinkRenderer(PREFIX);

  public AlpinistFrontend(ActorRef linkService, ActorRef tgService) {
    this.linkService = linkService;
    this.tgService = tgService;
  }

  public Route route() {
    return route(
      path("main.css", () -> getFromResource(
        "main.css",
        ContentTypes.create(MediaTypes.TEXT_CSS, HttpCharsets.UTF_8)
      )),
      path("main.js", () -> getFromResource(
        "main.js",
        ContentTypes.create(MediaTypes.APPLICATION_JAVASCRIPT, HttpCharsets.UTF_8)
      )),
      path("favicon.ico", () -> getFromResource(
        "favicon.ico",
        MediaTypes.IMAGE_X_ICON.toContentType()
      )),
      path("sync", () ->
        post(() -> complete(sync()))
      ),
      pathEndOrSingleSlash(() -> completeWithFuture(renderSpace("recent"))),
      path("alert", () -> parameter("message", message -> {
        tgService.tell(new Alert(Alert.Type.ALERT, message), ActorRef.noSender());
        return complete(StatusCodes.OK);
      })),
      pathPrefix("links", () -> concat(
        pathPrefix(name -> route(
          pathEnd(() -> completeWithFuture(renderSpace(name))),
          get(() -> path(LinkAction.EDIT.encoded, () -> completeWithFuture(renderEdit(name)))),
          post(() -> route(
            path(LinkAction.DELETE.encoded, () -> completeWithFuture(delete(name))),
            path(LinkAction.EDIT.encoded, () -> formFieldMap(map -> {
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
        ))
      ))
    );
  }

  public enum LinkAction {
    DELETE("delete"),
    EDIT("edit");

    public final String encoded;

    LinkAction(String encoded) {
      this.encoded = encoded;
    }
  }

  private CompletionStage<HttpResponse> delete(String name) {
    linkService.tell(new DeleteLink(name), ActorRef.noSender());
    return renderSpace(name);
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
    return Patterns.ask(linkService, new GetLink(name), TIMEOUT_MILLIS)
      .thenApply(response -> (Optional<Link>) response)
      .thenApply(l -> l.orElse(new Link(name, new LinkPayload(name, "", ""))))
      .thenApply(link -> renderBody(editLinkRenderer.render(link)));
  }

  private HttpResponse renderBody(String body) {
    return HttpResponse.create()
      .withEntity(ContentTypes.TEXT_HTML_UTF8, pageRenderer.render(body));
  }
}