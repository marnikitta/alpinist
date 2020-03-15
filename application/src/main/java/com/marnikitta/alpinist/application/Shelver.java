package com.marnikitta.alpinist.application;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.PatternsCS;
import com.marnikitta.alpinist.model.CommonTags;
import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.service.api.GetLinks;
import com.marnikitta.alpinist.service.api.UpdatePayload;
import com.marnikitta.alpinist.tg.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Shelver extends AbstractActor {
  private final ActorRef linkService;
  private final ActorRef notifySink;
  private final Logger log = LoggerFactory.getLogger(Shelver.class);

  private Cancellable pinger = null;

  private Shelver(ActorRef linkService, ActorRef notifySink) {
    this.linkService = linkService;
    this.notifySink = notifySink;
  }

  public static Props props(ActorRef linkService, ActorRef notifySink) {
    return Props.create(Shelver.class, linkService, notifySink);
  }

  @Override
  public void preStart() {
    this.pinger = getContext().getSystem().scheduler().schedule(
      FiniteDuration.apply(10, TimeUnit.SECONDS),
      FiniteDuration.apply(30, TimeUnit.MINUTES),
      self(),
      new Ping(),
      context().dispatcher(),
      self()
    );
    log.info("Started shelver pinger");
  }

  @Override
  public void postStop() {
    if (!pinger.isCancelled()) {
      log.info("Stopping shelver pinger");
      pinger.cancel();
    }
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
      .match(Ping.class, p -> handlePing())
      .build();
  }

  private void handlePing() {
    PatternsCS.ask(linkService, new GetLinks(CommonTags.UNREAD), 10000)
      .thenApply(links -> (List<Link>) links)
      .thenAccept(links -> {
        final Random rd = new Random();
        log.info("{} Unread links received", links.size());

        final Instant monthAgo = Instant.now()
          .minus(7 * 4, ChronoUnit.DAYS);
        for (Link link : links) {
          final Instant linkExpireDate = monthAgo.minus(rd.nextInt(7 * 2), ChronoUnit.DAYS);
          if (link.updated().compareTo(linkExpireDate) < 0) {
            linkService.tell(new UpdatePayload(link.name(), link.payload().markedAsShelved()), self());

            log.info("Link {} was shelved", link.name());
            notifySink.tell(new Alert(Alert.Type.MESSAGE, "Link '" + link.name() + "' was shelved"), self());
          }
        }
      });
  }

  private static class Ping {
  }
}
