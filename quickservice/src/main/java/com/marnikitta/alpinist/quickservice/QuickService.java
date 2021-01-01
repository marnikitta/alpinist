package com.marnikitta.alpinist.quickservice;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Status;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.model.LinkPayload;
import com.marnikitta.alpinist.preview.LinkPreviewService;
import com.marnikitta.alpinist.preview.Preview;
import com.marnikitta.alpinist.service.api.CreateLink;

import java.time.Duration;

public class QuickService extends AbstractActor {
  private final ActorRef linkService;
  private final ActorRef previewService;
  private final Duration TIMEOUT = Duration.ofSeconds(5);

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
      .build();
  }

  private void handleUrl(String url, ActorRef sender) {
    Patterns.ask(previewService, url, TIMEOUT)
      .thenApply(p -> (Preview) p)
      .exceptionally(throwable -> new Preview(url, url))
      .thenAccept(p -> {
        final LinkPayload payload = new LinkPayload(p.title(), url, "");
        createLink(Link.filefy(payload.title()), payload, sender);
      });
  }

  private void createLink(String name, LinkPayload payload, ActorRef sender) {
    Patterns.ask(linkService, new CreateLink(name, payload), TIMEOUT)
      .thenApply(link -> (Link) link)
      .handle((link, throwable) -> {
        if (throwable != null) {
          sender.tell(new Status.Failure(throwable), self());
        } else {
          sender.tell(new Status.Success(link), self());
        }
        return link;
      });
  }

  public static class QuickLink {
    public final String url;

    public QuickLink(String url) {
      this.url = url;
    }
  }
}