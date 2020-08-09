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
import akka.pattern.PatternsCS;
import com.marnikitta.alpinist.application.frontend.render.CachedLinkRenderer;
import com.marnikitta.alpinist.application.frontend.render.LinkRenderer;
import com.marnikitta.alpinist.application.frontend.render.PageRenderer;
import com.marnikitta.alpinist.application.frontend.render.PopularTagsRenderer;
import com.marnikitta.alpinist.application.frontend.render.SpaceRenderer;
import com.marnikitta.alpinist.application.frontend.render.TemplateLinkRenderer;
import com.marnikitta.alpinist.application.quasitree.QuasiTree;
import com.marnikitta.alpinist.model.CommonTags;
import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.model.LinkPayload;
import com.marnikitta.alpinist.service.api.DeleteLink;
import com.marnikitta.alpinist.service.api.GetLink;
import com.marnikitta.alpinist.service.api.GetLinks;
import com.marnikitta.alpinist.service.api.Sync;
import com.marnikitta.alpinist.service.api.UpdatePayload;
import com.marnikitta.alpinist.tg.Alert;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AlpinistFrontend extends AllDirectives {
  private static final String PREFIX = "";
  public static final Duration TIMEOUT_MILLIS = Duration.ofMillis(10000);
  private final ActorRef linkService;
  private final ActorRef tgService;

  private final PageRenderer pageRenderer = new PageRenderer(PREFIX);
  private final PopularTagsRenderer tagsRenderer = new PopularTagsRenderer(PREFIX);
  private final LinkRenderer linkRender = new CachedLinkRenderer(new TemplateLinkRenderer(PREFIX));
  private final SpaceRenderer spaceRenderer = new SpaceRenderer(PREFIX);

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
      path("favicon.ico", () -> getFromResource(
        "favicon.ico",
        MediaTypes.IMAGE_X_ICON.toContentType()
      )),
      path("sync", () ->
        post(() -> complete(sync()))
      ),
      pathEndOrSingleSlash(() -> completeWithFuture(renderRecent())),
      pathPrefix("tags", () -> route(
        pathEnd(() -> completeWithFuture(renderPopularTags())),
        pathPrefix(tag -> pathSingleSlash(() -> completeWithFuture(renderTag(tag))))
      )),
      path("alert", () -> parameter("message", message -> {
        tgService.tell(new Alert(Alert.Type.ALERT, message), ActorRef.noSender());
        return complete(StatusCodes.OK);
      })),
      pathPrefix("links", () ->
        pathPrefix(name -> route(
          pathEnd(() -> completeWithFuture(renderLink(name, false))),
          get(() -> path(LinkAction.EDIT.encoded, () -> completeWithFuture(renderLink(name, true)))),
          post(() -> route(
            path(LinkAction.DELETE.encoded, () -> completeWithFuture(delete(name))),
            path(LinkAction.READ.encoded, () -> completeWithFuture(applyAction(name, LinkAction.READ))),
            path(LinkAction.UNREAD.encoded, () -> completeWithFuture(applyAction(name, LinkAction.UNREAD))),
            path(LinkAction.SHELVE.encoded, () -> completeWithFuture(applyAction(name, LinkAction.SHELVE))),
            path(LinkAction.GOLDEN.encoded, () -> completeWithFuture(applyAction(name, LinkAction.GOLDEN))),
            path(LinkAction.UNGOLDEN.encoded, () -> completeWithFuture(applyAction(name, LinkAction.UNGOLDEN))),
            path(LinkAction.EDIT.encoded, () -> formFieldMap(map -> {
              final String discussion = map.get("discussion");
              final String tags = map.get("tags");

              if (discussion == null || tags == null) {
                throw new IllegalArgumentException("Expected discussion and tags params");
              }

              return completeWithFuture(edit(name, discussion, tags));
            }))
          ))
        ))
      )
    );
  }

  private CompletionStage<HttpResponse> applyAction(String name, LinkAction action) {
    return PatternsCS.ask(linkService, new GetLink(name), TIMEOUT_MILLIS)
      .thenApply(l -> (Link) l)
      .thenApply(link -> {
        final LinkPayload newPayload;
        switch (action) {
          case READ:
            newPayload = link.payload().markedAsRead();
            break;
          case UNREAD:
            newPayload = link.payload().markedAsUnread();
            break;
          case SHELVE:
            newPayload = link.payload().markedAsShelved();
            break;
          case GOLDEN:
            newPayload = link.payload().markedAsGolden();
            break;
          case UNGOLDEN:
            newPayload = link.payload().markedAsUngolden();
            break;
          default:
            throw new IllegalStateException("Some status update is not handled");
        }

        linkService.tell(new UpdatePayload(name, newPayload), ActorRef.noSender());
        return HttpResponse.create()
          .withStatus(StatusCodes.SEE_OTHER)
          .addHeader(Location.create(PREFIX + "/links/" + name));
      });
  }

  public enum LinkAction {
    READ("read"),
    UNREAD("unread"),
    SHELVE("shelve"),
    GOLDEN("golden"),
    DELETE("delete"),
    EDIT("edit"),
    UNGOLDEN("ungolden");

    public final String encoded;

    LinkAction(String encoded) {
      this.encoded = encoded;
    }
  }

  private CompletionStage<HttpResponse> delete(String name) {
    linkService.tell(new DeleteLink(name), ActorRef.noSender());
    return CompletableFuture.completedFuture(HttpResponse.create()
      .withStatus(StatusCodes.SEE_OTHER)
      .addHeader(Location.create(PREFIX + '/'))
    );
  }

  private CompletionStage<HttpResponse> edit(String name, String discussion, String tags) {
    return PatternsCS.ask(linkService, new GetLink(name), TIMEOUT_MILLIS)
      .thenApply(response -> (Link) response)
      .thenCompose(link -> {
        final Set<String> newTags = Stream.of(tags.split(",")).map(String::trim).collect(Collectors.toSet());
        final LinkPayload p = link.payload()
          .withNewDiscussion(discussion.replace("\r\n", "\n"))
          .withUpdatedTags(newTags);
        return PatternsCS.ask(linkService, new UpdatePayload(name, p), TIMEOUT_MILLIS);
      })
      .thenCompose(o -> CompletableFuture.completedFuture(HttpResponse.create()
          .withStatus(StatusCodes.SEE_OTHER)
          .addHeader(Location.create(PREFIX + "/links/" + name))
        )
      );
  }

  private CompletionStage<HttpResponse> renderRecent() {
    return Patterns.ask(linkService, new GetLinks(), TIMEOUT_MILLIS)
      .thenApply(response -> (List<Link>) response)
      .exceptionally(e -> List.of())
      .thenApply((List<Link> links) -> {
        final String body = links.stream()
          .map(linkRender::renderWithoutActions)
          .collect(Collectors.joining());

        return HttpResponse.create()
          .withEntity(ContentTypes.TEXT_HTML_UTF8, pageRenderer.render(body, "recent"));
      });
  }

  private CompletionStage<HttpResponse> renderPopularTags() {
    return popularTags()
      .thenApply(tags -> {
        final String body = tagsRenderer.render(tags, true);
        return HttpResponse.create()
          .withEntity(ContentTypes.TEXT_HTML_UTF8, pageRenderer.render(body, "tags"));
      });
  }

  private CompletionStage<List<String>> popularTags() {
    return Patterns.ask(linkService, new GetLinks(), TIMEOUT_MILLIS)
      .thenApply(response -> (List<Link>) response)
      .thenApply(this::extractedPopularTags)
      .exceptionally(e -> List.of());
  }

  private List<String> extractedPopularTags(List<Link> links) {
    final Map<String, Long> countByTag = links.stream()
      .flatMap(li -> li.payload().tags())
      .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    return countByTag.entrySet().stream()
      .sorted(Comparator.comparingLong(e -> -e.getValue()))
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());
  }

  private CompletionStage<HttpResponse> renderTag(String tag) {
    final CompletionStage<QuasiTree> quasiTree = Patterns.ask(linkService, new GetLinks("quasitree"), TIMEOUT_MILLIS)
      .thenApply(links -> (List<Link>) links)
      .thenApply(links -> {
        if (links.size() == 1) {
          return QuasiTree.fromLink(links.get(0).payload());
        } else {
          return new QuasiTree();
        }
      }).exceptionally(t -> new QuasiTree());

    return quasiTree.thenCompose(tree -> {
      final List<String> childTags = tree.child(tag).collect(Collectors.toList());
      childTags.add(tag);
      return Patterns.ask(linkService, new GetLinks(), TIMEOUT_MILLIS)
        .thenApply(result -> (List<Link>) result)
        .exceptionally(t -> List.of())
        .thenApply(links -> {
          final List<Link> filteredLinks = links.stream()
            .filter(l -> l.payload().tags().anyMatch(childTags::contains))
            .collect(Collectors.toList());

          final Optional<String> spaceBody = filteredLinks.stream()
            .filter(li1 -> li1.name().equals(tag) && li1.payload().tags().anyMatch(t -> t.equals(CommonTags.SPACE)))
            .findFirst().map(s -> spaceRenderer.render(
              s.name(),
              s.payload().discussion()
            ));

          final Optional<String> parent = tree.parent(tag);
          final List<String> childrenTags = extractedPopularTags(filteredLinks).stream()
            .filter(childTags::contains)
            .filter(t -> !t.equals(tag))
            .collect(Collectors.toList());

          final String tagsTree;
          if (parent.isPresent()) {
            tagsTree = tagsRenderer.renderTree(parent.get(), childrenTags);
          } else {
            tagsTree = tagsRenderer.render(childrenTags, true);
          }

          final String body = tagsTree
            + spaceBody.orElse("")
            + filteredLinks.stream().filter(li -> !li.name().equals(tag))
            .map(linkRender::renderWithoutActions)
            .collect(Collectors.joining());
          return renderBody(body, String.format("%s (%d)", tag, filteredLinks.size()));
        });
    });
  }

  private HttpResponse sync() {
    linkService.tell(new Sync(), ActorRef.noSender());
    return HttpResponse.create()
      .withStatus(StatusCodes.SEE_OTHER)
      .addHeader(Location.create(PREFIX + '/'));
  }

  private CompletionStage<HttpResponse> renderLink(String name, boolean edit) {
    return PatternsCS.ask(linkService, new GetLink(name), TIMEOUT_MILLIS)
      .thenApply(response -> (Link) response)
      .thenApply(link -> {
        final String body;
        if (edit) {
          body = linkRender.renderEdit(link);
        } else {
          body = linkRender.renderWithActions(link);
        }
        return HttpResponse.create()
          .withEntity(ContentTypes.TEXT_HTML_UTF8, pageRenderer.render(body));
      });
  }

  private HttpResponse renderBody(String body, String title) {
    return HttpResponse.create()
      .withEntity(ContentTypes.TEXT_HTML_UTF8, pageRenderer.render(body, title));
  }
}