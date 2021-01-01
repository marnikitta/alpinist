package com.marnikitta.alpinist.service;

import akka.actor.AbstractActor;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.model.LinkRepository;
import com.marnikitta.alpinist.service.api.CreateLink;
import com.marnikitta.alpinist.service.api.CreateOrUpdate;
import com.marnikitta.alpinist.service.api.DeleteLink;
import com.marnikitta.alpinist.service.api.GetLink;
import com.marnikitta.alpinist.service.api.GetSpace;
import com.marnikitta.alpinist.service.api.LinkSpace;
import com.marnikitta.alpinist.service.api.Sync;
import com.marnikitta.alpinist.service.api.UpdatePayload;
import scala.concurrent.duration.FiniteDuration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
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
    this.linkRepository = new CachingLinkRepository(new FileLinkRepository(baseDir));
  }

  private LinkService(LinkRepository linkRepository) {
    this.linkRepository = linkRepository;
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

  public static Props props(LinkRepository linkRepository) {
    return Props.create(LinkService.class, linkRepository);
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
        linkRepository.delete(delete.name());
        sender().tell(new Success(delete.name()), self());
      })
      .match(GetLink.class, getLink -> {
        sender().tell(link(getLink.name()), self());
      })
      .match(GetSpace.class, getSpace -> {
        if (getSpace.name().equals("recent")) {
          sender().tell(new LinkSpace("recent", linkRepository.links().collect(Collectors.toList())), self());
        } else {
          final List<Link> incoming = incomingLinks(getSpace.name());
          final LinkSpace space = link(getSpace.name()).map(l -> new LinkSpace(l, incoming))
            .orElse(new LinkSpace(getSpace.name(), incoming));
          sender().tell(space, self());
        }
      })
      .match(CreateOrUpdate.class, createOrUpdate -> {
        if (!createOrUpdate.name.equals(createOrUpdate.link.name())) {
          throw new IllegalArgumentException("cant rename links yet");
        }

        final Optional<Link> originalLink = link(createOrUpdate.name);
        if (createOrUpdate.link.payload().isEmpty()) {
          // Updated link is empty. It is useless now
          linkRepository.delete(createOrUpdate.name);
        } else if (originalLink.isPresent()
          && createOrUpdate.name.equals(createOrUpdate.link.name())
          && !originalLink.get().equals(createOrUpdate.link)) {
          // No renaming
          linkRepository.update(createOrUpdate.name, createOrUpdate.link.payload().withUpdatedNow());
        } else if (originalLink.isEmpty() && !createOrUpdate.link.payload().isEmpty()) {
          linkRepository.create(createOrUpdate.name, createOrUpdate.link.payload().withUpdatedNow());
        }
        sender().tell(true, self());
      })
      .build();
  }

  private Optional<Link> link(String name) {
    return linkRepository.links()
      .filter(l -> l.name().equals(name))
      .findAny();
  }

  private List<Link> incomingLinks(String name) {
    return linkRepository.links().filter(l -> l.payload().hasOutlink(name)).collect(Collectors.toList());
  }

  private Link linkOrThrow(String name) {
    return link(name).orElseThrow(() -> new NoSuchElementException("There is no link with name '" + name + '\''));
  }
}
