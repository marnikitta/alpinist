package com.marnikitta.alpinist.application;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import com.marnikitta.alpinist.application.frontend.AlpinistFrontend;
import com.marnikitta.alpinist.quickservice.QuickService;
import com.marnikitta.alpinist.service.LinkService;
import com.marnikitta.alpinist.tg.BotParams;
import com.marnikitta.alpinist.tg.TelegramService;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import scala.concurrent.duration.Duration;

import javax.annotation.Nullable;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class AlpinistApplication extends AbstractActor {
  private static final int PORT = 8080;
  private static final String HOST = "0.0.0.0";

  @Nullable
  private final BotParams params;

  @Nullable
  private final String remote;
  private final String localDir;

  public static void main(String[] args) throws ParseException {
    final Options options = new Options();
    options.addOption("r", "remote", true, "Remote repository url");
    options.addOption("d", "dir", true, "Local repository directory");
    options.addOption("u", "username", true, "Telegram bot username");
    options.addOption("t", "token", true, "Telegram bot token");
    options.addOption("o", "owner", true, "Telegram bot owner");

    final CommandLine commandLine = new DefaultParser().parse(options, args);

    final ActorSystem system = ActorSystem.create("alpinist");

    @Nullable final String remote = commandLine.getOptionValue("r");
    final String localDir = commandLine.getOptionValue("d", "./base");

    @Nullable final BotParams botParams;
    if (commandLine.hasOption("u") && commandLine.hasOption("t") && commandLine.hasOption("o")) {
      botParams = new BotParams(
        commandLine.getOptionValue("u"),
        commandLine.getOptionValue("t"),
        Long.parseLong(commandLine.getOptionValue("o"))
      );
    } else if (commandLine.hasOption("u") || commandLine.hasOption("t") || commandLine.hasOption("o")) {
      System.err.println("All three options --username, --token, --owner should be set. Or none of them");
      botParams = null;
      System.exit(0);
    } else {
      System.out.println("I don't have bot params");
      botParams = null;
    }

    system.actorOf(AlpinistApplication.props(
      remote,
      localDir,
      botParams
    ), "application");
  }

  private AlpinistApplication(@Nullable String remote, String localDir, @Nullable BotParams params) {
    this.remote = remote;
    this.params = params;
    this.localDir = localDir;
  }

  public static Props props(@Nullable String remote, String localDir, @Nullable BotParams params) {
    return Props.create(AlpinistApplication.class, remote, localDir, params);
  }

  @Override
  public void preStart() {
    final Materializer materializer = ActorMaterializer.create(context());

    final ActorRef linkService;
    if (remote != null) {
      linkService = context().actorOf(LinkService.props(remote, Paths.get(localDir)), "link");
    } else {
      linkService = context().actorOf(LinkService.props(Paths.get(localDir)), "link");
    }

    final ActorRef quickService = context().actorOf(QuickService.props(linkService), "quick");

    final ActorRef tgService;
    if (params != null) {
      tgService = context().actorOf(TelegramService.props(params, quickService), "tg");
    } else {
      tgService = context().system().deadLetters();
    }

    final AlpinistFrontend frontend = new AlpinistFrontend(linkService, tgService);
    context().actorOf(Shelver.props(linkService, tgService), "shelver");

    Http.get(context().system())
      .bindAndHandle(
        frontend.route().flow(context().system(), materializer),
        ConnectHttp.toHost(HOST, PORT),
        materializer
      );
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create().build();
  }

  @Override
  public SupervisorStrategy supervisorStrategy() {
    return OneForOneStrategy.apply(
      1000,
      Duration.apply(1, TimeUnit.HOURS),
      true,
      DeciderBuilder
        .match(RuntimeException.class, e -> SupervisorStrategy.restart())
        .matchAny(e -> SupervisorStrategy.escalate())
        .build()
    );
  }
}
