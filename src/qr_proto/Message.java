package qr_proto;

public class Message {
  private String message;

  public Message(String message) {
    this(message, false);
  }

  public Message(String message, boolean addEnd) {
    this.message = message;
    if(addEnd)
      this.message += "\\0";
  }

  public String removeSubstring(int beginIndex, int endIndex) {
    String start = message.substring(beginIndex, endIndex);
    message = message.substring(endIndex);
    return start;
  }

  public void escape (){
    message = message.replace("\\", "\\b");
  }

  public void unescape (){
    message = message.replace("\\b", "\\");
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return getMessage();
  }
}
