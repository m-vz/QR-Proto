package qr_proto;

public class Message {
  public static final String MESSAGE_END = "\\0";

  private String message;

  public Message(String message) {
    this.message = message;
  }

  public String removeSubstring(int beginIndex, int endIndex) {
    String start = message.substring(beginIndex, endIndex);
    message = message.substring(endIndex);
    return start;
  }

  public Message escape (){
    message = message.replace("\\", "\\b") + MESSAGE_END;
    return this;
  }

  public Message unescape (){
    message = message.substring(0, message.length() - MESSAGE_END.length()).replace("\\b", "\\");
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
