package qr_proto;

import java.awt.*;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.ThreadFactory;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import qr_proto.exception.ParsingException;
import qr_proto.gui.QRProtoPanel;
import qr_proto.qr.QRCode;

/**
 * Created by Aeneas on 18.04.18.
 */
public class QRProtoSocket {
  private static final int MAX_BUFFER_SIZE = 2953;
  private static final int SENDER_SLEEP_TIME = 1000, RECEIVER_SLEEP_TIME = 1000;

  private volatile boolean shouldClose = false, connecting = false, connected = false;
  private volatile int currentSequenceNumber = 1;
  private LinkedList<Message> messageQueue, sendMessagesQueue;
  private QRProtoPanel panel;
  private Webcam webcam;
  private Thread senderThread, receiverThread;

  public QRProtoSocket() {
    messageQueue = new LinkedList<>();
    sendMessagesQueue = new LinkedList<>();

    Dimension size = WebcamResolution.VGA.getSize();
    panel = new QRProtoPanel(size.height);

    webcam = Webcam.getWebcams().get(0);

    senderThread = new Thread(new QRProtoSocketSender());
    senderThread.start();

    receiverThread = new Thread(new QRProtoSocketReceiver());
    receiverThread.start();
  }

  public void sendMessage(String message) {
    messageQueue.add(new Message(message, true));
  }

  public void connect() {
    connecting = true;

    sendQRCode(QRCode.SYN);
  }

  private void sendQRCode(QRCode qrCode) {
    panel.displayQRCode(qrCode); // TODO: this is temporary and needs to be expanded.
  }

  private void parseMessage(Message message) throws ParsingException {
    String content = message.getMessage();
    int contentLength = content.length();

    int sequenceNumber = ByteBuffer.wrap(Base64.getDecoder().decode(content.substring(0, 8))).getInt();
    String ackmessage = content.substring(contentLength - 6, contentLength - 4);
    byte checksum = Base64.getDecoder().decode(content.substring(contentLength - 4))[0];

    content = content.substring(8, contentLength - 6);
    contentLength = content.length();

    if(checksum != QRCode.checksum(content))
      throw new ParsingException("Error: Checksum not identical!");

    if(sequenceNumber == 0 && sequenceNumber != currentSequenceNumber+1)
      return; // TODO: handle with more style.

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
          shouldClose = true;

          connected = false;
        }
    } else { // content message
      System.out.println("Received content message:\n" + content);
    }

  }

  QRProtoPanel getPanel() {
    return panel;
  }

  private class QRProtoSocketSender implements Runnable {
    @Override
    public void run() {
      Message message;
      int remainingBufferSize = MAX_BUFFER_SIZE;

      do {
        try {
          Thread.sleep(RECEIVER_SLEEP_TIME);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        if (!messageQueue.isEmpty()) {
          if (messageQueue.peek().getMessage().length() > remainingBufferSize) {
            message = new Message(messageQueue.peek().removeSubstring(0, remainingBufferSize));
          } else {
            message = messageQueue.poll();
          }

          message.escape();

          System.out.println("Sending qr code with sequence number " + currentSequenceNumber + " and content:\n" + message);
          sendQRCode(new QRCode(currentSequenceNumber, message, QRCode.AcknowledgementMessage.END));
          synchronized(this) { currentSequenceNumber++; }
        }
      } while (!shouldClose);

      sendQRCode(QRCode.FIN);
    }
  }

  class QRProtoSocketReceiver implements Runnable {
    @Override
    public void run() {
      do {
        try {
          Thread.sleep(SENDER_SLEEP_TIME);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        BufferedImage image;
        Result result = null;

        if(webcam.isOpen()) {
          if((image = webcam.getImage()) == null)
            continue;

          LuminanceSource source = new BufferedImageLuminanceSource(image);
          BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

          try {
            result = new MultiFormatReader().decode(bitmap);
          } catch (NotFoundException e) {
            // no qr code found
          }
        }

        if(result != null) {
          Message message = new Message(result.getText());
          message.unescape();
          System.out.println("Received qr code with message:\n" + message);
          try {
            parseMessage(message);
          } catch(ParsingException e) {
            e.printStackTrace();
          }
        }
      } while(!shouldClose);
    }
  }
}
