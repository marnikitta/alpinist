package com.marnikitta.alpinist.repository;

import com.marnikitta.alpinist.model.LinkPayload;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkEncoder {
  private static final Pattern URL_TITLE = Pattern.compile(
    "# \\[(?<urltitle>\\p{Print}+)]\\((?<url>\\p{Print}+)\\)\n"
  );
  private static final Pattern TITLE = Pattern.compile(
    "# (?<title>\\p{Print}+)\n"
  );
  private static final Pattern UPDATED = Pattern.compile("(__Updated:__ (?<datetime>[\\p{Digit}T.\\-:]+)\n)?");
  private static final Pattern DISCUSSION = Pattern.compile("(?<discussion>[\\p{Print}\n]*)");
  @SuppressWarnings("RegExpDuplicateAlternationBranch")
  private static final Pattern LINK = Pattern.compile(
    "^((" + URL_TITLE.pattern() + ")|(" + TITLE.pattern() + "))\n*" +
      UPDATED.pattern() + "\n*" +
      DISCUSSION.pattern() + '$', Pattern.UNICODE_CHARACTER_CLASS
  );

  public String encode(LinkPayload linkPayload) {
    final StringBuilder sb = new StringBuilder();
    if (linkPayload.url().isEmpty()) {
      sb.append(String.format("# %s\n", linkPayload.title()));
    } else {
      sb.append(String.format("# [%s](%s)\n", linkPayload.title(), linkPayload.url()));
    }

    final String dateString = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(linkPayload.updated());
    sb.append("\n__Updated:__ ").append(dateString).append("\n");

    sb.append('\n').append(linkPayload.rawDiscussion());

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
      final String discussion = matcher.group("discussion");

      final LocalDateTime updated = Optional.ofNullable(matcher.group("datetime"))
        .map(t -> LocalDateTime.parse(t, DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .orElse(null);
      return new LinkPayload(title, url, discussion, updated);
    } else {
      throw new IllegalArgumentException("String can't be parsed");
    }
  }
}
