package com.marnikitta.alpinist.model;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

public final class CommonTags {
  private static int tagsPriority(Set<String> tags) {
    if (tags.contains(CommonTags.UNREAD)) {
      return 3;
    } else if (tags.contains(SHELVED)) {
      return 2;
    } else {
      return 1;
    }
  }

  public static Comparator<LinkPayload> READ_UNREAD_COMPARATOR = (o1, o2) -> {
    final Set<String> tags1 = o1.tags().collect(Collectors.toSet());
    final Set<String> tags2 = o2.tags().collect(Collectors.toSet());

    return Integer.compare(tagsPriority(tags1), tagsPriority(tags2));
  };

  public static final String UNREAD = "unread";
  public static final String LINK = "link";
  public static final String GOLDEN = "golden";
  public static final String LOGBOOK = "logbook";
  public static final String SHELVED = "shelved";
  public static final String SPACE = "space";
  public static final String NOTE = "note";
  public static final String CONF = "conf";
}
