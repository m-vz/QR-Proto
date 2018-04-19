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
    System.out.println("Escaping message content from:\n" + message);
    message = message.replace("\\", "\\b") + MESSAGE_END;
    System.out.println("To:\n" + message);
    return this;
  }

  public Message unescape (){
    System.out.println("Unescaping message content from:\n" + message);
    message = message.substring(message.length() - MESSAGE_END.length()).replace("\\b", "\\");
    System.out.println("To:\n" + message);
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
