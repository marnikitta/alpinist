package com.marnikitta.alpinist.application;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.PatternsCS;
import com.marnikitta.alpinist.model.CommonTags;
import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.service.api.GetLinks;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IdeaService extends AbstractActor {
  private final ActorRef linkService;

  private IdeaService(ActorRef linkService) {
    this.linkService = linkService;
  }

  public static Props props(ActorRef linkService) {
    return Props.create(IdeaService.class, linkService);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
      .match(GetIdeas.class, this::getIdeas)
      .build();
  }

  private void getIdeas(GetIdeas msg) {
    final ActorRef sender = sender();
    PatternsCS.ask(linkService, new GetLinks(CommonTags.LOGBOOK), 10000)
      .thenApply(links -> (List<Link>) links)
      .thenAccept(links -> {
        links.sort(Link.CHRONO_ORDER);
        final List<Idea> ideas = links.stream().flatMap(this::extractIdeas).collect(Collectors.toList());
        sender.tell(ideas, self());
      });
  }

  private Stream<Idea> extractIdeas(Link link) {
    final List<Idea> ideas = new ArrayList<>();
    final String[] lines = link.payload().discussion().split("\n");
    for (String line : lines) {
      if (line.startsWith("Идея:") || line.startsWith("Idea:")) {
        final String ideaText = line.substring("Идея:".length()).trim();
        ideas.add(new Idea(link.name(), ideaText));
      }
    }
    return ideas.stream();
  }

  public static class GetIdeas {
  }

  public static class Idea {
    public final String sourceName;
    public final String text;

    public Idea(String sourceName, String text) {
      this.sourceName = sourceName;
      this.text = text;
    }
  }
}
