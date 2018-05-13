package qr_proto.util;

import qr_proto.qr.Message;
import qr_proto.qr.QRCode;

public class Log {
  public static final boolean DISPLAY_MESSAGES = false;

  public static synchronized void err(String message) {
    System.err.print(message);
  }

  public static synchronized void out(String message) {
    System.out.print(message);
  }

  public static synchronized void errln(String message) {
    System.err.println(message);
  }

  public static synchronized void outln(String message) {
    System.out.println(message);
  }

  public static synchronized void outln(QRCode code) {
    out("QRCode: {sequenceNumber: " + code.getSequenceNumber() + ", type: " + code.getType() + ", acknowledgementMessage: " + code.getAcknowledgementMessage());
    if(code.getMessages().isEmpty())
      outln("} without any messages.");
    else {
      outln("} with messages" + (DISPLAY_MESSAGES ? ":" : "."));
      if(DISPLAY_MESSAGES)
        for(Message message: code.getMessages())
          outln(message.getMessage());
    }
  }

  public static synchronized void outln(Message message) {
    outln("Message with content: " + message.getMessage());
  }
}
