package qr_proto;

public class Message {
  public static final String MESSAGE_END = "\\0";

  private String message;
  private boolean complete, escaped;

  public Message(String message, boolean complete, boolean escaped) {
    this.message = message;
    this.complete = complete;
    this.escaped = escaped;
  }

  public Message(String message, boolean complete) {
    this(message, complete, false);
  }

  public String removeSubstring(int beginIndex, int endIndex) {
    String start = message.substring(beginIndex, endIndex);
    message = message.substring(endIndex);
    return start;
  }

  public Message escape (){
    if(!escaped) {
      message = message.replace("\\", "\\b");
      if(complete)
        message += MESSAGE_END;
      escaped = true;
    }
    return this;
  }

  public Message unescape () {
    if(escaped) {
      int length = message.length();
      if(complete)
        length -= MESSAGE_END.length();
      message = message.substring(0, length).replace("\\b", "\\");
      escaped = false;
    }
    return this;
  }

  public String getMessage() {
    return message;
  }

  boolean isEscaped() {
    return escaped;
  }

  @Override
  public String toString() {
    return getMessage();
  }
}
