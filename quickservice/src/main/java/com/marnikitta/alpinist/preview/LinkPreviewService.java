package com.marnikitta.alpinist.preview;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static akka.actor.Status.Failure;

public class LinkPreviewService extends AbstractActor {
  private final Logger logger = LoggerFactory.getLogger(LinkPreviewService.class);
  private final OkHttpClient client = new OkHttpClient().newBuilder()
    .connectTimeout(2, TimeUnit.SECONDS)
    .writeTimeout(3, TimeUnit.SECONDS)
    .readTimeout(3, TimeUnit.SECONDS)
    .build();

  public static Props props() {
    return Props.create(LinkPreviewService.class);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
      .match(String.class, url -> {
        try {
          final Request request = new Request.Builder()
            .url(url)
            .build();

          final Response response = client.newCall(request).execute();
          final ResponseBody body = response.body();
          if (body != null) {
            final String payload = body.string();
            final Optional<String> title = title(payload);
            if (title.isPresent()) {
              sender().tell(new Preview(title.get(), payload), self());
            } else {
              sender().tell(new Failure(new IllegalArgumentException("Can't locate title")), self());
            }
          } else {
            sender().tell(new Failure(new IllegalArgumentException("Unable to fetch page")), self());
          }
        } catch (Exception e) {
          logger.error("Smth went wrong during processing of " + url, e);
          sender().tell(new Failure(e), self());
        }
      })
      .build();
  }

  private Optional<String> title(String html) {
    final Pattern compile = Pattern.compile("<title>(.*)</title>");
    final Matcher matcher = compile.matcher(html);
    if (matcher.find()) {
      return Optional.of(matcher.group(1));
    } else {
      return Optional.empty();
    }
  }
}
