package qr_proto;

import qr_proto.qr.QRCode;

public class Log {
  public static final Log log = new Log();

  public synchronized void err(String message) {
    System.err.print(message);
  }

  public synchronized void out(String message) {
    System.out.print(message);
  }

  public synchronized void errln(String message) {
    System.err.println(message);
  }

  public synchronized void outln(String message) {
    System.out.println(message);
  }

  public synchronized void outln(QRCode code) {
    out("QRCode: {sequenceNumber: " + code.getSequenceNumber() + ", type: " + code.getType() + ", acknowledgementMessage: " + code.getAcknowledgementMessage());
    if(code.getMessages().isEmpty())
      outln("} without any messages.");
    else {
      outln("} with messages:");
      for(Message message: code.getMessages())
        outln(message.getMessage());
    }
  }

  public synchronized void outln(Message message) {
    outln("Message with content: " + message.getMessage());
  }
}
