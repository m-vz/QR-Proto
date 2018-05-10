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
    if(message.contains("\\b")) {
      int a = 5;
    }
    if(escaped) {
      int length = getMessageLength();
      if(complete)
        length -= MESSAGE_END.length();
      message = message.substring(0, length).replace("\\b", "\\");
      escaped = false;
    }
    return this;
  }

  public int getMessageLength() {
    return message.length();
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return getMessage();
  }
}
