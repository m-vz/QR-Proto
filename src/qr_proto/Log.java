package qr_proto;

import qr_proto.qr.QRCode;

public class Log {
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
      outln("} with messages:");
      for(Message message: code.getMessages())
        outln(message.getMessage());
    }
  }

  public static synchronized void outln(Message message) {
    outln("Message with content: " + message.getMessage());
  }
}
