package com.marnikitta.alpinist.model;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;

public class Link implements Comparable<Link> {
  public static final Comparator<Link> CHRONO_ORDER = Comparator
    .comparing(Link::updated)
    .thenComparing(Link::created)
    .thenComparing(Link::name);

  public static final Comparator<Link> SERP_ORDER = CHRONO_ORDER.reversed();

  private final String name;
  private final Instant created;
  private final Instant updated;
  private final LinkPayload payload;

  public Link(String name, Instant created, Instant updated, LinkPayload payload) {
    this.updated = updated;
    this.name = name;
    this.created = created;
    this.payload = payload;
  }

  public String name() {
    return name;
  }

  public Instant created() {
    return created;
  }

  public LinkPayload payload() {
    return payload;
  }

  public Instant updated() {
    return updated;
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
      Objects.equals(created, link.created) &&
      Objects.equals(updated, link.updated) &&
      Objects.equals(payload, link.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, created, updated, payload);
  }

  @Override
  public String toString() {
    return "Link{" +
      "name='" + name + '\'' +
      ", created=" + created +
      ", updated=" + updated +
      ", payload=" + payload +
      '}';
  }

  public static String filefy(String title) {
    final Pattern pattern = Pattern.compile("[^\\p{Alnum} ]", Pattern.UNICODE_CHARACTER_CLASS);
    return pattern.matcher(title.toLowerCase()).replaceAll("")
      .replaceAll("\\s+", " ")
      .trim()
      .replaceAll(" ", "_");
  }
}
