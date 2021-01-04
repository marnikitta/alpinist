package com.marnikitta.alpinist.model;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Link implements Comparable<Link> {
  public static final Comparator<Link> SERP_ORDER = Comparator
    .comparing(Link::payload)
    .thenComparing(Link::name).reversed();

  private final String name;
  private final LinkPayload payload;

  public Link(String name, LinkPayload payload) {
    this.name = name;
    this.payload = payload;
  }

  public String name() {
    return name;
  }

  public LinkPayload payload() {
    return payload;
  }

  @Override
  public int compareTo(Link o) {
    return SERP_ORDER.compare(this, o);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Link link = (Link) o;
    return Objects.equals(name, link.name) &&
      Objects.equals(payload, link.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, payload);
  }

  @Override
  public String toString() {
    return "Link{" +
      "name='" + name + '\'' +
      ", payload=" + payload +
      '}';
  }

  public static String filefy(String title) {
    final String rawCleanedTitle = title.replaceAll("https://", "")
      .replaceAll("http://", "")
      .replaceAll(".pdf", "")
      .replaceAll("www.", "")
      .replaceAll(".com", "")
      .replaceAll(".net", "")
      .replaceAll(".ru", "")
      .replaceAll("/", " ")
      .replaceAll("-", " ")
      .replaceAll("_", " ")
      .replaceAll("\\.", " ");

    final Pattern pattern = Pattern.compile("[^\\p{Alnum} ]", Pattern.UNICODE_CHARACTER_CLASS);
    final String result = pattern.matcher(rawCleanedTitle.toLowerCase())
      .replaceAll("")
      .replaceAll("\\s+", " ")
      .trim()
      .replaceAll(" ", "_");

    final String shortenedResult = Arrays.stream(result.split("_")).limit(8).collect(Collectors.joining("_"));

    if (shortenedResult.isBlank()) {
      return UUID.randomUUID().toString();
    }

    return shortenedResult;
  }
}
