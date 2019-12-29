package com.marnikitta.alpinist.service;

import com.marnikitta.alpinist.model.LinkPayload;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LinkEncoder {
  private static final Pattern URL_TITLE = Pattern.compile(
    "# \\[(?<urltitle>\\p{Print}+)]\\((?<url>\\p{Print}+)\\)\n"
  );
  private static final Pattern TITLE = Pattern.compile(
    "# (?<title>\\p{Print}+)\n"
  );
  private static final Pattern TAGS = Pattern.compile("(__Tags:__ (?<tags>\\p{Print}+)\n)?");
  private static final Pattern DISCUSSION = Pattern.compile("(?<discussion>[\\p{Print}\n]*)");
  @SuppressWarnings("RegExpDuplicateAlternationBranch")
  private static final Pattern LINK = Pattern.compile(
    "^((" + URL_TITLE.pattern() + ")|(" + TITLE.pattern() + "))\n*" +
      TAGS.pattern() + "\n*" +
      DISCUSSION.pattern() + '$', Pattern.UNICODE_CHARACTER_CLASS
  );

  public String encode(LinkPayload linkPayload) {
    final StringBuilder sb = new StringBuilder();
    if (linkPayload.url().isEmpty()) {
      sb.append(String.format("# %s\n", linkPayload.title()));
    } else {
      sb.append(String.format("# [%s](%s)\n", linkPayload.title(), linkPayload.url()));
    }
    final Set<String> tags = linkPayload.tags().collect(Collectors.toSet());
    if (!tags.isEmpty()) {
      sb.append(tags.stream().collect(Collectors.joining(", ", "\n__Tags:__ ", "\n")));
    }
    sb.append('\n').append(linkPayload.discussion());

    return sb.toString();
  }

  public LinkPayload decode(String raw) {
    final Matcher matcher = LINK.matcher(raw);
    if (matcher.matches()) {
      final String title;
      final String url;
      if (matcher.group("urltitle") == null) {
        title = matcher.group("title");
        url = "";
      } else {
        title = matcher.group("urltitle");
        url = matcher.group("url");
      }
      final Set<String> tags = Optional.ofNullable(matcher.group("tags"))
        .map(t -> Arrays.stream(t.split(",")).map(String::trim).collect(Collectors.toSet()))
        .orElse(Collections.emptySet());

      final String discussion = matcher.group("discussion");
      return new LinkPayload(title, url, tags, discussion);
    } else {
      throw new IllegalArgumentException("String can't be parsed");
    }
  }
}
