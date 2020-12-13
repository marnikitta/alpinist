package com.marnikitta.alpinist.tg;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.PatternsCS;
import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.quickservice.QuickService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.MessageEntity;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

public class TelegramService extends AbstractActor {
  private final Logger log = LoggerFactory.getLogger(TelegramService.class);
  private final ActorRef quickService;
  private final BotParams params;

  private TelegramLongPollingBot bot;

  private TelegramService(BotParams params, ActorRef quickService) {
    this.quickService = quickService;
    this.params = params;
  }

  public static Props props(BotParams params, ActorRef quickService) {
    return Props.create(TelegramService.class, params, quickService);
  }

  @Override
  public void preStart() throws TelegramApiRequestException {
    ApiContextInitializer.init();
    final TelegramBotsApi api = new TelegramBotsApi();
    this.bot = new TelegramLongPollingBot() {
      @Override
      public String getBotToken() {
        return params.token();
      }

      @Override
      public void onUpdateReceived(Update update) {
        self().tell(update, ActorRef.noSender());
      }

      @Override
      public String getBotUsername() {
        return params.username();
      }
    };
    api.registerBot(bot);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
      .match(Alert.class, alert -> {
        bot.execute(new SendMessage(params.ownerId(), alert.toString()));
      })
      .match(Update.class, update -> {
        log.info("Received update '{}'", update);
        if (update.hasMessage()) {
          final Message message = update.getMessage();
          if (message.isUserMessage() && message.getFrom().getId() == params.ownerId()) {

            if (message.hasEntities()) {
              for (MessageEntity e : message.getEntities()) {
                if (e.getType().equals("url")) {
                  final String url = message.getText().substring(e.getOffset(), e.getOffset() + e.getLength());
                  handleUrl(url);
                  return;
                }
              }
            }
          } else {
            log.warn("Illegal access");
          }
        }
      })
      .build();
  }

  private void handleUrl(String url) {
    PatternsCS.ask(quickService, new QuickService.QuickLink(url), 10000)
      .thenApply(link -> (Link) link)
      .whenComplete(this::onLinkComplete);
  }

  private void onLinkComplete(Link link, Throwable e) {
    if (e != null) {
      ooops(e);
      return;
    }

    try {
      bot.execute(new SendMessage(params.ownerId(), "Updated: " + link.name()));
    } catch (TelegramApiException e1) {
      ooops(e1);
    }
  }

  private void ooops(Throwable e) {
    try {
      bot.execute(new SendMessage(params.ownerId(), e.getMessage()));
    } catch (TelegramApiException ignored) {
      log.error("Something went very wrong", e);
    }
  }
}