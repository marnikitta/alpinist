package com.marnikitta.alpinist.tg;

public class BotParams {
  private final String username;
  private final String token;
  private final long ownerId;

  public BotParams(String username, String token, long ownerId) {
    this.username = username;
    this.token = token;
    this.ownerId = ownerId;
  }

  public String username() {
    return username;
  }

  public String token() {
    return token;
  }

  public long ownerId() {
    return ownerId;
  }
}
