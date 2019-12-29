package com.marnikitta.alpinist.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LinkGenerator implements Supplier<LinkPayload> {
  private final Random rd;

  private final List<String> tags = Arrays.asList(
    CommonTags.UNREAD, "paper", "book", "diary", CommonTags.LOGBOOK, "quicklink", "quicknote", "personal", "math", "buzzword"
  );

  public LinkGenerator(Random rd) {
    this.rd = rd;
  }

  public LinkGenerator() {
    this(new Random());
  }

  public Link getLink() {
    return new Link(nextWord(), Instant.now(), Instant.now(), get());
  }

  @Override
  public LinkPayload get() {
    final String title = nextSentence();
    final String url = rd.nextBoolean()
      ? "https://" + nextWord() + ".com/" + nextWord()
      : "";
    final Set<String> tags = Stream.generate(this::nextTag)
      .limit(rd.nextInt(5))
      .collect(Collectors.toSet());
    final String discussion = Stream.generate(this::nextSentence)
      .limit(rd.nextInt(20))
      .collect(Collectors.joining("\n"));
    return new LinkPayload(title, url, tags, discussion);
  }

  public String nextSentence() {
    final int size = rd.nextInt(10) + 1;
    return Stream.generate(this::nextWord).limit(size).collect(Collectors.joining(" "));
  }

  public String nextTag() {
    return tags.get(ThreadLocalRandom.current().nextInt(tags.size()));
  }

  public String nextWord() {
    final int size = rd.nextInt(10) + 10;
    final char[] word = new char[size];
    for (int i = 0; i < word.length; i++) {
      word[i] = (char) (rd.nextInt('z'- 'a') + 'a');
    }
    return new String(word);
  }

  public Link updatedLink(Link link) {
    return new Link(link.name(), link.created(), Instant.now(), get());
  }
}
