package com.marnikitta.alpinist.model;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LinkPayload {
  private final String title;
  private final String url;
  private final Set<String> tags;
  private final String discussion;

  public LinkPayload(String title,
                     Set<String> tags,
                     String discussion) {
    this.url = "";
    this.tags = tags;
    this.title = title;
    this.discussion = discussion;
  }

  public LinkPayload(String title,
                     String url,
                     Set<String> tags,
                     String discussion) {
    this.url = url;
    this.tags = tags;
    this.title = title;
    this.discussion = discussion;
  }

  public String title() {
    return title;
  }

  public String url() {
    return url;
  }

  public String discussion() {
    return discussion;
  }

  public Stream<String> tags() {
    return tags.stream();
  }

  public LinkPayload withUpdatedTags(Set<String> newTags) {
    return new LinkPayload(title, url, newTags, discussion);
  }

  public LinkPayload withAddedTags(String... tags) {
    final Set<String> newTags = tags().collect(Collectors.toSet());
    newTags.addAll(Arrays.asList(tags));
    return withUpdatedTags(newTags);
  }

  public LinkPayload withRemovedTags(String... tags) {
    final Set<String> newTags = tags().collect(Collectors.toSet());
    newTags.removeAll(Arrays.asList(tags));
    return withUpdatedTags(newTags);
  }

  public LinkPayload markedAsRead() {
    return withRemovedTags(CommonTags.UNREAD);
  }

  public LinkPayload markedAsUnread() {
    return withRemovedTags(CommonTags.SHELVED).withAddedTags(CommonTags.UNREAD);
  }

  public LinkPayload markedAsGolden() {
    return withAddedTags(CommonTags.GOLDEN);
  }

  public LinkPayload markedAsUngolden() {
    return withRemovedTags(CommonTags.GOLDEN);
  }

  public LinkPayload markedAsShelved() {
    return withRemovedTags(CommonTags.UNREAD).withAddedTags(CommonTags.SHELVED);
  }

  public LinkPayload withNewDiscussion(String discussion) {
    return new LinkPayload(title, url, tags, discussion);
  }

  public LinkPayload withLines(List<String> newLines) {
    final String newDiscussion = discussion + newLines.stream().collect(Collectors.joining("\n\n", "\n", "\n"));
    return new LinkPayload(title, url, tags, newDiscussion);
  }

  @Override
  public String toString() {
    return "LinkPayload{" +
      "title='" + title + '\'' +
      ", url='" + url + '\'' +
      ", tags=" + tags +
      ", discussion='" + discussion + '\'' +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final LinkPayload linkPayload = (LinkPayload) o;
    return Objects.equals(title, linkPayload.title) &&
      Objects.equals(url, linkPayload.url) &&
      Objects.equals(discussion, linkPayload.discussion) &&
      Objects.equals(tags, linkPayload.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, url, discussion, tags);
  }
}
