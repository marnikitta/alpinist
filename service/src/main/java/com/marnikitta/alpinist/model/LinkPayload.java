package com.marnikitta.alpinist.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LinkPayload implements Comparable<LinkPayload> {
  private final String title;
  private final String url;
  @Nullable
  private final LocalDateTime updated;
  private final String discussion;

  private final Set<String> outlinks;

  public static final Pattern OUTLINK_PATTERN = Pattern.compile(
    "\\[\\[(?<outlink>[\\p{Lower}\\p{Digit}_\\-]+)]]",
    Pattern.UNICODE_CHARACTER_CLASS
  );

  public LinkPayload(String title,
                     String url,
                     String discussion) {
    this.url = url;
    this.title = title;
    this.discussion = discussion;
    this.updated = LinkPayload.now();
    this.outlinks = allOutlinks(discussion);
  }

  public LinkPayload(String title,
                     String url,
                     String discussion,
                     @Nullable LocalDateTime updated) {
    this.url = url;
    this.title = title;
    this.discussion = discussion;
    this.updated = updated;
    this.outlinks = allOutlinks(discussion);
  }

  public String title() {
    return title;
  }

  public String url() {
    return url;
  }

  public LocalDateTime updated() {
    return updated != null ? updated : LocalDateTime.MIN;
  }

  public LinkPayload withUpdatedNow() {
    return new LinkPayload(this.title, this.url, this.discussion, LinkPayload.now());
  }

  public LinkPayload withUpdatedDiscussion(String discussion) {
    return new LinkPayload(this.title, this.url, discussion, this.updated);
  }

  public String rawDiscussion() {
    return discussion;
  }

  public String renderedDiscussion(String prefix) {
    return renderedDiscussion(outlink -> "[[[" + outlink + "]]](" + prefix + outlink + ")");
  }

  public String renderedDiscussion(Function<String, String> linksReplacer) {
    final List<String> outlinks = this.outlinks().collect(Collectors.toList());

    String result = discussion;
    for (String outlink : outlinks) {
      result = result.replace("[[" + outlink + "]]", linksReplacer.apply(outlink));
    }

    return result;
  }

  public boolean hasOutlink(String name) {
    return this.outlinks.contains(name);
  }

  public Stream<String> outlinks() {
    return this.outlinks.stream();
  }

  private static Set<String> allOutlinks(String discussion) {
    final Set<String> result = new HashSet<>();
    final Matcher matcher = OUTLINK_PATTERN.matcher(discussion);
    while (matcher.find()) {
      final String outlink = matcher.group("outlink");
      result.add(outlink);
    }
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final LinkPayload payload = (LinkPayload) o;
    return Objects.equals(title, payload.title) &&
      Objects.equals(url, payload.url) &&
      Objects.equals(discussion, payload.discussion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, url, discussion);
  }

  @Override
  public int compareTo(@NotNull LinkPayload o) {
    return this.updated().compareTo(o.updated());
  }

  @Override
  public String toString() {
    return "LinkPayload{" +
      "title='" + title + '\'' +
      ", url='" + url + '\'' +
      ", discussion='" + discussion + '\'' +
      '}';
  }

  public boolean isEmpty() {
    return discussion.isEmpty() && url.isEmpty();
  }

  public static LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.ofHours(3));
  }
}
