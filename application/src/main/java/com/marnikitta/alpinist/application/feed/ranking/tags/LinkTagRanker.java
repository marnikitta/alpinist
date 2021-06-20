package com.marnikitta.alpinist.application.feed.ranking.tags;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.marnikitta.alpinist.ml.Model;
import com.marnikitta.alpinist.ml.RandomModel;
import com.marnikitta.alpinist.model.Link;

import java.util.ArrayList;
import java.util.List;

public class LinkTagRanker extends AbstractActor {
  private final Model rankingModel = new RandomModel();
  private final List<Link> knownLinks = new ArrayList<>();
  private final ActorRef linkService;

  private LinkTagRanker(ActorRef linkService) {
    this.linkService = linkService;
  }

  public static Props props(ActorRef linkService) {
    return Props.create(LinkTagRanker.class, linkService);
  }

  private List<String> sortCandidates(Link link, List<String> candidates, List<Link> allLinks) {
    final List<LinkTagItem> items = new ArrayList<>();
    for (String tag : candidates) {
      items.add(new LinkTagItem(link, tag));
    }

    return new ArrayList<>(candidates);
  }

  private void setWindowCounts(List<LinkTagItem> items, List<Link> links) {
    links.sort(Link.SERP_ORDER);
    for (LinkTagItem item : items) {
      for (int window : new int[]{1, 5, 10, 20, 50, 100, 200, 500, 1000}) {
        //links.stream().limit(window).map(link -> link.payload().outlinks().filter(t -> t.equals()))
      }
    }
  }

  @Override
  public Receive createReceive() {
    return null;
  }
}
