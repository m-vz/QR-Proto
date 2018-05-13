package qr_proto.util;

import qr_proto.qr.Message;
import qr_proto.qr.QRCode;

public class Log {
  public static final boolean DISPLAY_MESSAGES_DEFAULT = false;

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
    outln(code, DISPLAY_MESSAGES_DEFAULT);
  }

  public static synchronized void outln(QRCode code, boolean displayMessages) {
    out("QRCode: {sequenceNumber: " + code.getSequenceNumber() + ", type: " + code.getType() + ", acknowledgementMessage: " + code.getAcknowledgementMessage());
    if(code.getMessages().isEmpty())
      outln("} without any messages.");
    else {
      outln("} with messages" + (displayMessages ? ":" : "."));
      if(displayMessages)
        for(Message message: code.getMessages())
          outln(message.getMessage());
    }
  }

  public static synchronized void outln(int sequenceNumber, QRCode.QRCodeType type, QRCode.AcknowledgementMessage acknowledgementMessage, String content) {
    outln(sequenceNumber, type, acknowledgementMessage, content, DISPLAY_MESSAGES_DEFAULT);
  }

  public static synchronized void outln(int sequenceNumber, QRCode.QRCodeType type, QRCode.AcknowledgementMessage acknowledgementMessage, String content, boolean displayMessages) {
    outln(
        "QRCode: {sequenceNumber: " + sequenceNumber +
            ", type: " + type +
            ", acknowledgementMessage: " + acknowledgementMessage +
            (content.length() > 0 ? " with content" + (displayMessages ? ":\n" + content : ".") : " without any content.")
    );
  }

  public static synchronized void outln(Message message) {
    outln("Message with content: " + message.getMessage());
  }
}
