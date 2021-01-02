package com.marnikitta.alpinist.kv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marnikitta.alpinist.model.LinkPayload;

import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KV {
  private final static ObjectMapper mapper = new ObjectMapper();

  private final Map<String, String> cache = new HashMap<>();
  private LocalDateTime lastUpdated;

  private static final Pattern DISCUSSION_PATTERN = Pattern.compile(
    "^```json\n(?<body>[\\p{Print}\n]+)\n```\n\n\\[\\[hidden]]$"
  );

  public KV() {
    this.lastUpdated = LinkPayload.now();
  }

  public KV(LinkPayload discussion) {
    this.cache.putAll(parseDiscussion(discussion.rawDiscussion()));
    this.lastUpdated = discussion.updated().orElse(LinkPayload.now());
  }

  private static Map<String, String> parseDiscussion(String discussion) {
    final Matcher matcher = DISCUSSION_PATTERN.matcher(discussion);
    if (!matcher.matches()) {
      throw new IllegalArgumentException();
    }

    final String body = matcher.group("body");

    try {
      //noinspection Convert2Diamond
      return mapper.readValue(body, new TypeReference<Map<String, String>>() {});
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  public LinkPayload toPayload() {
    try {
      final String body = mapper.writerWithDefaultPrettyPrinter()
        .writeValueAsString(this.cache);
      final String discussion = "```json\n" + body + "\n```\n\n[[hidden]]";

      return new LinkPayload("KV", "", discussion, this.lastUpdated);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void put(String key, String value) {
    lastUpdated = LinkPayload.now();
    this.cache.put(key, value);
  }

  public Optional<String> get(String key) {
    return Optional.ofNullable(this.cache.get(key));
  }
}
