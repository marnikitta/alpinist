package com.marnikitta.alpinist.preview;

public class Preview {
  private final String title;
  private final String body;

  public Preview(String title, String body) {
    this.title = title;
    this.body = body;
  }

  public String title() {
    return title;
  }

  public String body() {
    return body;
  }
}
