package com.marnikitta.alpinist.tg;

public class Alert {
  public final Alert.Type type;
  public final String message;

  public Alert(Alert.Type type, String message) {
    this.type = type;
    this.message = message;
  }

  @Override
  public String toString() {
    if (this.type == Type.MESSAGE) {
      return this.message;
    } else {
      return '[' + type.toString() + "] " + message;
    }
  }

  public enum Type {
    ALERT,
    MESSAGE,
    NOTIFY
  }
}
