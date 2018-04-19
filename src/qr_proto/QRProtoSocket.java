package qr_proto;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Vector;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import qr_proto.gui.QRProtoPanel;
import qr_proto.qr.QRCode;

import javax.swing.*;

/**
 * Created by Aeneas on 18.04.18.
 */
public class QRProtoSocket {
  private static final int MAX_BUFFER_SIZE = 2953;
  private static final int SENDER_SLEEP_TIME = 10, RECEIVER_SLEEP_TIME = 10, DISPLAY_TIME = 1000;

  private volatile boolean shouldClose = false, connecting = false, connected = false;
  private volatile int currentSequenceNumber = 1;
  private LinkedList<Message> messageQueue;
  private LinkedList<QRCode> sentQRCodes;
  private AbstractAction connectedCallback = null;
  private QRProtoPanel panel;
  private Webcam webcam;
  private Thread senderThread, receiverThread;

  public QRProtoSocket() {
    messageQueue = new LinkedList<>();
    sentQRCodes = new LinkedList<>();

    Dimension size = WebcamResolution.VGA.getSize();
    panel = new QRProtoPanel(size.height);

    webcam = Webcam.getWebcams().get(0);

    senderThread = new Thread(new QRProtoSocketSender());
    senderThread.start();

    receiverThread = new Thread(new QRProtoSocketReceiver());
    receiverThread.start();
  }

  public void sendMessage(String message) {
    messageQueue.add(new Message(message));
  }

  public void connect(AbstractAction callback) {
    if(connected) {
      System.err.println("Already connected.");
      return;
    }

    System.out.println("Connecting...");

    connectedCallback = callback;
    connecting = true;

    sendQRCode(QRCode.SYN);
  }

  public void disconnect() {
    if(!connected) {
      System.err.println("Not connected.");
      return;
    }

    System.out.println("Disconnecting...");

    connected = false;
    messageQueue = new LinkedList<>();
    sentQRCodes = new LinkedList<>();

    sendQRCode(QRCode.FIN);

    System.out.println("Disconnected.");
  }

  private void sendQRCode(QRCode qrCode) {
    panel.displayQRCode(qrCode); // TODO: this is temporary and needs to be expanded.
    sentQRCodes.add(qrCode);

    try {
      Thread.sleep(DISPLAY_TIME);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void parseMessage(Message message) {
    String content = message.getMessage();
    int contentLength = content.length();

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

          if(connectedCallback != null)
            connectedCallback.actionPerformed(new ActionEvent(this, 0, "connected")); // TODO: can the action event be composed of more useful information?

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

  boolean isConnected() {
    return connected;
  }

  private class QRProtoSocketSender implements Runnable {
    @Override
    public void run() {
      Message message;
      int remainingBufferSize = MAX_BUFFER_SIZE;

      do {
        try {
          Thread.sleep(SENDER_SLEEP_TIME);
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
          Thread.sleep(RECEIVER_SLEEP_TIME);
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
          String content = result.getText();
          int contentLength = content.length();

          int sequenceNumber = ByteBuffer.wrap(Base64.getDecoder().decode(content.substring(0, 8))).getInt();
          String acknowledgementMessage = content.substring(contentLength - 6, contentLength - 4); // TODO: nothing is currently being done with this.
          byte checksum = Base64.getDecoder().decode(content.substring(contentLength - 4))[0];

          if(checksum != QRCode.checksum(content.substring(0, contentLength - 4))) {
            System.err.println("Error: Checksum not identical!");
            continue; // TODO: currently ignoring messages with incorrect checksums.
          }

          if(sequenceNumber != 0 && sequenceNumber != currentSequenceNumber+1)
            return; // TODO: handle with more style.
          synchronized(this) {
            currentSequenceNumber++;
          }

          content = content.substring(8, contentLength - 6);
          contentLength = content.length();

          Vector<Message> messages = new Vector<>();
          int current = 0, next;
          while((next = content.indexOf(Message.MESSAGE_END, current)) != -1) {
            next += Message.MESSAGE_END.length();
            messages.add(new Message(content.substring(current, next)));
            current = next;
          }
          content = content.substring(current, contentLength); // this is the remaining content that is not a complete message

          for(Message message: messages) {
            System.out.println("Received message with content:\n" + message);
            message.unescape();
            System.out.println("Unescaped content to:\n" + message);

            parseMessage(message);
          }
        }
      } while(!shouldClose);
    }
  }
}
