package com.marnikitta.alpinist.application.frontend;

import akka.actor.ActorRef;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import com.marnikitta.alpinist.tg.Alert;

public class UtilFrontend extends AllDirectives {
  private final ActorRef tgService;

  public UtilFrontend(ActorRef tgService) {
    this.tgService = tgService;
  }

  public Route alertRoute() {
    return path("alert", () -> parameter("message", message -> {
      tgService.tell(new Alert(Alert.Type.ALERT, message), ActorRef.noSender());
      return complete(StatusCodes.OK);
    }));
  }
}
