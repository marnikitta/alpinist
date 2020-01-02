package com.marnikitta.alpinist.service;

import akka.actor.AbstractActor;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.model.LinkRepository;
import com.marnikitta.alpinist.service.api.CreateLink;
import com.marnikitta.alpinist.service.api.DeleteLink;
import com.marnikitta.alpinist.service.api.Exists;
import com.marnikitta.alpinist.service.api.GetLink;
import com.marnikitta.alpinist.service.api.GetLinks;
import com.marnikitta.alpinist.service.api.Sync;
import com.marnikitta.alpinist.service.api.UpdatePayload;
import scala.concurrent.duration.FiniteDuration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static akka.actor.Status.Failure;
import static akka.actor.Status.Success;

public class LinkService extends AbstractActor {
  private static final FiniteDuration SYNC_PERIOD = FiniteDuration.apply(5, TimeUnit.MINUTES);
  private final LinkRepository linkRepository;

  private Cancellable cronSync = null;

  private LinkService(String remote, Path baseDir) throws IOException {
    this.linkRepository = new CachingLinkRepository(GitLinkRepository.createFromRemote(remote, baseDir));
  }

  private LinkService(Path baseDir) {
    this.linkRepository = new CachingLinkRepository(GitLinkRepository.createFromDirectory(baseDir));
  }

  @Override
  public void preStart() {
    linkRepository.sync();
    cronSync = context().system()
      .scheduler()
      .schedule(SYNC_PERIOD, SYNC_PERIOD, self(), new Sync(), context().dispatcher(), self());
  }

  @Override
  public void postStop() {
    if (cronSync != null && !cronSync.isCancelled()) {
      cronSync.cancel();
    }
  }

  public static Props props(String remote, Path basePath) {
    return Props.create(LinkService.class, remote, basePath);
  }

  public static Props props(Path basePath) {
    return Props.create(LinkService.class, basePath);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
      .match(Sync.class, s -> {
        linkRepository.sync();
      })
      .match(CreateLink.class, create -> {
        try {
          final Link link = linkRepository.create(create.name(), create.payload());
          sender().tell(link, self());
        } catch (IllegalArgumentException e) {
          sender().tell(new Failure(e), self());
        }
      })
      .match(Exists.class, exists -> {
        try {
          linkOrThrow(exists.name());
          sender().tell(true, self());
        } catch (NoSuchElementException e) {
          sender().tell(false, self());
        }
      })
      .match(UpdatePayload.class, updatePayload -> {
        try {
          final Link updated = linkRepository.update(
            updatePayload.name(),
            updatePayload.newPayload()
          );
          sender().tell(updated, self());
        } catch (NoSuchElementException e) {
          sender().tell(new Failure(e), self());
        }
      })
      .match(DeleteLink.class, delete -> {
        try {
          linkRepository.delete(delete.name());
          sender().tell(new Success(delete.name()), self());
        } catch (NoSuchElementException e) {
          sender().tell(new Failure(e), self());
        }
      })
      .match(GetLink.class, getLink -> {
        try {
          sender().tell(linkOrThrow(getLink.name()), self());
        } catch (NoSuchElementException e) {
          sender().tell(new Failure(e), self());
        }
      })
      .match(GetLinks.class, getLinks -> {
        final List<Link> links = linkRepository.links()
          .filter(l -> {
            final Set<String> tags = l.payload().tags().collect(Collectors.toSet());
            final Set<String> requestTags = getLinks.tags().collect(Collectors.toSet());
            return tags.containsAll(requestTags);
          })
          .skip(getLinks.offset())
          .limit(getLinks.limit())
          .collect(Collectors.toList());
        sender().tell(links, self());
      })
      .build();
  }

  private Link linkOrThrow(String name) {
    return linkRepository.links()
      .filter(l -> l.name().equals(name))
      .findAny().orElseThrow(() -> new NoSuchElementException("There is no link with name '" + name + '\''));
  }
}
