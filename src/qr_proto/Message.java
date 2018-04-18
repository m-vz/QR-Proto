package qr_proto;

public class Message {
  private String message;

  Message(String message) {
    this.message = message;
  }

  public String removeSubstring(int beginIndex, int endIndex) {
    String start = message.substring(beginIndex, endIndex);
    message = message.substring(endIndex);
    return start;
  }
}
