package qr_proto.qr;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class Message {
  public static final String MESSAGE_END = "\\0";
//  public static final boolean COMPRESS = true;

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
    if(escaped) {
//      if(COMPRESS) {
//        try {
//          InputStreamReader reader = new InputStreamReader(new BrotliInputStream(new ByteArrayInputStream(message.getBytes("UTF-8"))));
//          StringBuilder builder = new StringBuilder();
//          int r;
//          while((r = reader.read()) != -1)
//            builder.append((char) r);
//          message = builder.toString();
//        } catch (UnsupportedEncodingException e) {
//          e.printStackTrace();
//        } catch(IOException e) {
//          e.printStackTrace();
//        }
//      }
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
