package com.marnikitta.alpinist.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LinkGenerator implements Supplier<LinkPayload> {
  public Link getLink() {
    return new Link(nextWord(), get());
  }

  @Override
  public LinkPayload get() {
    final String title = nextSentence();
    final String url = ThreadLocalRandom.current().nextBoolean()
      ? "https://" + nextWord() + ".com/" + nextWord()
      : "";
    final String discussion = Stream.generate(this::nextSentence)
      .limit(ThreadLocalRandom.current().nextInt(20))
      .collect(Collectors.joining("\n"));

    final LocalDateTime updated;
    if (ThreadLocalRandom.current().nextBoolean()) {
      updated = randomLocalDatetime();
    } else {
      updated = null;
    }

    return new LinkPayload(title, url, discussion, updated);
  }

  private LocalDateTime randomLocalDatetime() {
    final long minSecond = LocalDateTime.of(1970, 1, 1, 0, 0).toEpochSecond(ZoneOffset.UTC);
    final long maxSecond = LocalDateTime.of(2015, 1, 1, 0, 0).toEpochSecond(ZoneOffset.UTC);
    long randomDay = ThreadLocalRandom.current().nextLong(minSecond, maxSecond);
    return LocalDateTime.ofEpochSecond(randomDay, 0, ZoneOffset.UTC);
  }

  public String nextSentence() {
    final int size = ThreadLocalRandom.current().nextInt(10) + 1;
    return Stream.generate(this::nextWord).limit(size).collect(Collectors.joining(" "));
  }

  public String nextWord() {
    final int size = ThreadLocalRandom.current().nextInt(10) + 10;
    final char[] word = new char[size];
    for (int i = 0; i < word.length; i++) {
      word[i] = (char) (ThreadLocalRandom.current().nextInt('z' - 'a') + 'a');
    }
    return new String(word);
  }

  public Link updatedLink(Link link) {
    return new Link(link.name(), get());
  }
}
