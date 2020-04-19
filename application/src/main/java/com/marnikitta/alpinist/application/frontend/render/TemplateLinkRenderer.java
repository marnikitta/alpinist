package com.marnikitta.alpinist.application.frontend.render;

import com.marnikitta.alpinist.model.CommonTags;
import com.marnikitta.alpinist.model.Link;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.marnikitta.alpinist.application.frontend.AlpinistFrontend.LinkAction;

public class TemplateLinkRenderer implements LinkRenderer {
  private final Template linkTemplate = new Template("links/link.html");
  private final Template editLinkTemplate = new Template("links/edit.html");
  private final Template tagTemplate = new Template("links/tag.html");
  private final Template actionTemplate = new Template("links/action.html");
  private final String prefix;

  private final MutableDataSet options = new MutableDataSet();
  private final Parser parser = Parser.builder(options).build();
  private final HtmlRenderer renderer = HtmlRenderer.builder(options).build();

  public TemplateLinkRenderer(String prefix) {
    this.prefix = prefix;
  }

  @Override
  public String renderWithoutActions(Link link) {
    return render(link, false, false);
  }

  @Override
  public String renderWithActions(Link link) {
    return render(link, true, false);
  }

  @Override
  public String renderEdit(Link link) {
    return render(link, true, true);
  }

  private String render(Link link, boolean showActions, boolean edit) {
    final String url;
    if (!link.payload().url().isEmpty()) {
      url = link.payload().url();
    } else {
      url = prefix + "/links/" + link.name();
    }
    final Map<String, String> vars = new HashMap<>();
    if (showActions) {
      vars.put("hidden", "");
    } else {
      vars.put("hidden", "hidden");
    }
    vars.put("url", url);
    vars.put("prefix", prefix);
    if (!edit) {
      vars.put("actions", renderActions(link.name(), link.payload().tags().collect(Collectors.toSet())));
    }
    vars.put("name", link.name());
    vars.put("title", link.payload().title());

    if (!edit) {
      vars.put("discussion", renderMd(link.payload().discussion()));
    } else {
      vars.put("discussion", link.payload().discussion());
    }

    vars.put("lastModifiedShort", link.created().toString());
    vars.put("lastModifiedLong", link.created().toString());
    final String tags;
    if (!edit) {
      tags = link.payload().tags().map(this::renderTag).collect(Collectors.joining(", "));
    } else {
      tags = link.payload().tags().collect(Collectors.joining(", "));
    }
    vars.put("tags", tags);

    if (edit) {
      return editLinkTemplate.render(vars);
    } else {
      return linkTemplate.render(vars);
    }
  }

  private String renderActions(String linkName, Set<String> tags) {
    final StringBuilder actions = new StringBuilder();
    actions.append(renderAction(linkName, "Edit", LinkAction.EDIT, false));
    if (tags.contains(CommonTags.GOLDEN)) {
      actions.append(renderAction(linkName, "Ungolden", LinkAction.UNGOLDEN, true));
    } else {
      actions.append(renderAction(linkName, "Golden", LinkAction.GOLDEN, true));
    }
    if (tags.contains(CommonTags.UNREAD) && !tags.contains(CommonTags.SHELVED)) {
      actions.append(renderAction(linkName, "Mark as read", LinkAction.READ, true));
      actions.append(renderAction(linkName, "Shelve", LinkAction.SHELVE, true));
    }
    if (!tags.contains(CommonTags.UNREAD)) {
      actions.append(renderAction(linkName, "Mark as unread", LinkAction.UNREAD, true));
    }
    actions.append(renderAction(linkName, "Delete", LinkAction.DELETE, true));
    return actions.toString();
  }

  public String renderAction(String linkName, String name, LinkAction action, boolean post) {
    final Map<String, String> vars = new HashMap<>();
    vars.put("name", linkName);
    vars.put("action", action.encoded);
    vars.put("actionName", name);
    vars.put("prefix", prefix);

    if (post) {
      vars.put("method", "post");
    } else {
      vars.put("method", "get");
    }

    return actionTemplate.render(vars);
  }

  private String renderTag(String tag) {
    final Map<String, String> vars = new HashMap<>();
    vars.put("prefix", prefix);
    vars.put("tag", tag);
    return tagTemplate.render(vars);
  }

  public String renderMd(String md) {
    final Node document = parser.parse(md);
    return renderer.render(document);
  }
}
