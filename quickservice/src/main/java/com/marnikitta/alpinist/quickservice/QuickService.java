package com.marnikitta.alpinist.quickservice;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Status;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.PatternsCS;
import com.google.common.collect.Sets;
import com.marnikitta.alpinist.model.CommonTags;
import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.model.LinkPayload;
import com.marnikitta.alpinist.preview.LinkPreviewService;
import com.marnikitta.alpinist.preview.Preview;
import com.marnikitta.alpinist.service.api.CreateLink;
import com.marnikitta.alpinist.service.api.Exists;
import com.marnikitta.alpinist.service.api.GetLink;
import com.marnikitta.alpinist.service.api.UpdatePayload;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Set;

public class QuickService extends AbstractActor {
  private static final Set<String> LINK_TAGS = Sets.newHashSet(CommonTags.LINK, CommonTags.UNREAD);
  private static final Set<String> NOTE_TAGS = Sets.newHashSet(CommonTags.LOGBOOK);

  private final ActorRef linkService;
  private final ActorRef previewService;

  private QuickService(ActorRef linkService) {
    this.linkService = linkService;
    this.previewService = context().actorOf(LinkPreviewService.props(), "preview");
  }

  public static Props props(ActorRef linkService) {
    return Props.create(QuickService.class, linkService);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
      .match(QuickLink.class, link -> handleUrl(link.url, sender()))
      .match(QuickNote.class, note -> handleNote(note, sender()))
      .build();
  }

  private void handleNote(QuickNote note, ActorRef sender) {
    final LocalDate date = LocalDate.now(ZoneOffset.ofHours(3));
    final DateTimeFormatter pattern =  DateTimeFormatter.ofPattern("yyyy-'W'ww");
    final String formattedDate = date.format(pattern);
    final String logBookId = "lb-" + formattedDate;

    PatternsCS.ask(linkService, new Exists(logBookId), 10000)
      .thenApply(flag -> (boolean) flag)
      .thenCompose(f -> {
        if (f) {
          return PatternsCS.ask(linkService, new GetLink(logBookId), 10000);
        } else {
          return PatternsCS.ask(
            linkService,
            new CreateLink(logBookId, new LinkPayload(formattedDate, NOTE_TAGS, "")),
            10000
          );
        }
      })
      .thenApply(l -> (Link) l)
      .thenCompose(link -> {
        final LinkPayload updated = link.payload().withLines(Collections.singletonList(note.body));
        return PatternsCS.ask(linkService, new UpdatePayload(link.name(), updated), 10000);
      })
      .whenComplete((v, error) -> {
        if (error != null) {
          sender.tell(new Status.Failure(error), self());
          return;
        }

        if (v != null) {
          sender.tell(v, self());
          return;
        }
      });
  }

  private void handleUrl(String url, ActorRef sender) {
    PatternsCS.ask(previewService, url, 10000).thenApply(p -> (Preview) p)
      .exceptionally(throwable -> new Preview(url, url))
      .thenAccept(p -> {
        final LinkPayload payload = new LinkPayload(p.title(), url, LINK_TAGS, "");
        createLink(Link.filefy(payload.title()), payload, sender);
      });
  }

  private void createLink(String name, LinkPayload payload, ActorRef sender) {
    PatternsCS.ask(linkService, new CreateLink(name, payload), 10000)
      .thenApply(link -> (Link) link)
      .thenAccept(link -> sender.tell(new Status.Success(link), self()))
      .handle((aVoid, throwable) -> {
        if (throwable != null) {
          sender.tell(new Status.Failure(throwable), self());
        } else {
          sender.tell(new Status.Failure(new RuntimeException(
            "Something went wrong during link creation, but I dunno what"
          )), self());
        }
        return aVoid;
      });
  }

  public static class QuickLink {
    public final String url;

    public QuickLink(String url) {
      this.url = url;
    }
  }

  public static class QuickNote {
    public final String body;

    public QuickNote(String body) {
      this.body = body;
    }
  }
}