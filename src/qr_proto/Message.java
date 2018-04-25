package qr_proto;

public class Message {
  public static final String MESSAGE_END = "\\0";

  private String message;
  private boolean complete;

  public Message(String message, boolean complete) {
    this.message = message;
    this.complete = complete;
  }

  public String removeSubstring(int beginIndex, int endIndex) {
    String start = message.substring(beginIndex, endIndex);
    message = message.substring(endIndex);
    return start;
  }

  public Message escape (){
    message = message.replace("\\", "\\b");
    if(complete)
      message += MESSAGE_END;
    return this;
  }

  public Message unescape () {
    int length = message.length();
    if(complete)
      length -= MESSAGE_END.length();
    message = message.substring(0, length).replace("\\b", "\\");
    return this;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return getMessage();
  }
}
