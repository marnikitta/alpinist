package com.marnikitta.alpinist.kv;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.service.api.CreateOrUpdate;
import com.marnikitta.alpinist.service.api.GetLink;
import com.sun.istack.Nullable;
import scala.concurrent.duration.FiniteDuration;
import scala.util.Failure;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class KVService extends AbstractActorWithStash {
  private static final FiniteDuration DUMP_PERIOD = FiniteDuration.apply(5, TimeUnit.MINUTES);
  private final ActorRef linkService;

  @Nullable
  private KV kv = null;

  private Cancellable cronSync = null;

  private KVService(ActorRef linkService) {
    this.linkService = linkService;
  }

  public static Props props(ActorRef linkService) {
    return Props.create(KVService.class, linkService);
  }

  @Override
  public void preStart() {
    cronSync = context().system()
      .scheduler()
      .schedule(DUMP_PERIOD, DUMP_PERIOD, self(), "sync", context().dispatcher(), self());

    Patterns.retry(() -> Patterns.ask(this.linkService, new GetLink("kv"),
      Duration.ofSeconds(1)
      ).thenApply(l -> (Optional<Link>) l),
      10, Duration.ofSeconds(5),
      context().system().scheduler(),
      context().dispatcher()
    ).thenAccept(l -> {
      final KV kv = l.map(li -> new KV(li.payload())).orElse(new KV());
      self().tell(kv, ActorRef.noSender());
    }).exceptionally(t -> {
      self().tell(PoisonPill.getInstance(), ActorRef.noSender());
      return null;
    });
  }

  @Override
  public void postStop() {
    if (cronSync != null && !cronSync.isCancelled()) {
      cronSync.cancel();
    }
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
      .match(KV.class, kv -> {
        this.kv = kv;
        unstashAll();
      })
      .match(String.class, s -> {
        if (notConstructed()) {
          return;
        }
        sync();
      })
      .match(GetValue.class, v -> {
        if (notConstructed()) {
          return;
        }
        sender().tell(kv.get(v.key), self());
      })
      .match(SetValue.class, v -> {
        if (notConstructed()) {
          return;
        }
        kv.put(v.key, v.value);
        sender().tell(kv.get(v.key), self());
      })
      .match(IncrementValue.class, v -> {
        if (notConstructed()) {
          return;
        }
        incrementValue(v.key);
      })
      .build();
  }

  private void incrementValue(String key) {
    final String value = kv.get(key).orElse("0");
    final int intValue;
    try {
      intValue = Integer.parseInt(value);
    } catch (NumberFormatException e) {
      sender().tell(new Failure<>(e), self());
      return;
    }

    kv.put(key, String.valueOf(intValue + 1));
    sender().tell(kv.get(key), self());
  }

  private boolean notConstructed() {
    if (this.kv == null) {
      stash();
      return true;
    }
    return false;
  }

  private void sync() {
    linkService.tell(new CreateOrUpdate("kv", new Link("kv", kv.toPayload())), sender());
  }

  public static class GetValue {
    public final String key;

    public GetValue(String key) {
      this.key = key;
    }
  }

  public static class SetValue {
    public final String key;
    public final String value;

    public SetValue(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }

  public static class IncrementValue {
    public final String key;

    public IncrementValue(String key) {
      this.key = key;
    }
  }
}
