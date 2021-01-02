package com.marnikitta.alpinist.kv;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.marnikitta.alpinist.InMemoryLinkRepository;
import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.model.LinkPayload;
import com.marnikitta.alpinist.service.LinkService;
import com.marnikitta.alpinist.service.api.GetLink;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import scala.util.Failure;

import java.util.Optional;


public class KVServiceTest {
  private ActorSystem system;

  @BeforeSuite
  public void setup() {
    this.system = ActorSystem.create();
  }

  @AfterSuite
  public void teardown() {
    TestKit.shutdownActorSystem(system);
    system = null;
  }

  @Test
  public void testWR() {
    final TestKit probe = new TestKit(system);
    final ActorRef linkService = system.actorOf(LinkService.props(new InMemoryLinkRepository()));
    final ActorRef subject = system.actorOf(KVService.props(linkService));

    subject.tell(new KVService.SetValue("k", "v"), probe.getRef());
    probe.expectMsg(Optional.of("v"));

    subject.tell(new KVService.GetValue("k"), probe.getRef());
    probe.expectMsg(Optional.of("v"));

    subject.tell(new KVService.GetValue("r"), probe.getRef());
    probe.expectMsg(Optional.empty());

    subject.tell("sync", probe.getRef());
    probe.expectMsg(true);

    linkService.tell(new GetLink("kv"), probe.getRef());
    final Optional<Link> link = probe.expectMsgClass(Optional.class);
    final LinkPayload payload = link.get().payload();

    final KV kv = new KV(payload);
    Assert.assertEquals(kv.get("k"), Optional.of("v"));

    for (int i = 0; i < 10000; ++i) {
      subject.tell(new KVService.IncrementValue("cntr"), probe.getRef());
      probe.expectMsg(Optional.of(String.valueOf(i + 1)));
    }

    subject.tell(new KVService.SetValue("test", "value"), probe.getRef());
    probe.expectMsg(Optional.of("value"));
    subject.tell(new KVService.IncrementValue("test"), probe.getRef());
    probe.expectMsgClass(Failure.class);
  }
}