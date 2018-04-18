package qr_proto;

import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.ThreadFactory;
import qr_proto.exception.ParsingException;
import qr_proto.qr.QRCode;

/**
 * Created by Aeneas on 18.04.18.
 */
public class QRProtoSocket implements Runnable, ThreadFactory {

  private static final int MAX_BUFFER_SIZE = 2953;

  private boolean shouldClose = false, connecting = false, connected = false;
  private volatile int currentSequenceNumber = 1;
  private LinkedList<Message> messageQueue, sendMessagesQueue;

  public QRProtoSocket() {
    messageQueue = new LinkedList<>();
    sendMessagesQueue = new LinkedList<>();

  }

  public void sendMessage(String message) {
    messageQueue.add(new Message(message));
  }

  public void connect() {
    connecting = true;
  }

  private void sendQRCode(QRCode qrCode) {

  }

  private void parseMessage(Message message) throws ParsingException {
    String content = message.getMessage();
    int contentLength = content.length();
    int sequenceNumber = Integer.parseInt(content.substring(0, 4));
    String ackmessage = content.substring(contentLength - 3, contentLength - 1);
    byte checksum = Byte.parseByte(content.substring(contentLength - 1));
    content = content.substring(4, contentLength - 3);
    contentLength = content.length();

    if(checksum != QRCode.checksum(content))
      throw new ParsingException("Error: Checksum not identical!");

    if(sequenceNumber == 0 && sequenceNumber != currentSequenceNumber+1) {
      return; // TODO: handle with more style.
    }

    if(!connected && !connecting) {
      if(contentLength == 6 && content.substring(0, 6).equals("\\m SYN")) { // connection is being established
        sendQRCode(QRCode.SCK);

        connecting = true;
      }
    } else if(connecting) {
      if(contentLength == 6 && content.substring(0, 2).equals("\\m")) { // connection message
        String msg = content.substring(3, 6);

        if(msg.equals("SCK")) { // reply with ack
          sendQRCode(QRCode.ACK);

          connecting = false;
          connected = true;
        } else if(msg.equals("ACK")) { // connection has been established
          connecting = false;
          connected = true;
        }
      }
    } else if(contentLength >= 6 && content.substring(0, 2).equals("\\m")) { // socket message
        String msg = content.substring(3, 6);

        if(msg.equals("ACK")) { // message acknowledgement received
//          int i = 6;
//          do {
//            currentSequenceNumber++;
//          } while((i + 5) < contentLength);
//
//          if(sequenceNumber > currentSequenceNumber+1)
        } else if(msg.equals("FIN")) {
          connected = false;

          shouldClose();
        }
    } else { // content message

    }

  }

  @Override
  public void run() {
    String m;
    int remainingBufferSize = MAX_BUFFER_SIZE;

    do {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      if (!messageQueue.isEmpty()) {
        if (messageQueue.peek().getMessage().length() > remainingBufferSize) {
          m = messageQueue.peek().removeSubstring(0, remainingBufferSize);
        } else {
          m = messageQueue.poll().getMessage();
        }


      }
    } while (!shouldClose);

    sendQRCode(QRCode.FIN);
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread t = new Thread(r, "qr-proto-socket");
    t.setDaemon(true);
    return t;
  }

  public void shouldClose() {
    shouldClose = true;
  }
}
