package qr_proto;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Vector;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import qr_proto.gui.QRProtoPanel;
import qr_proto.qr.QRCode;

import javax.swing.*;
import qr_proto.qr.QRCode.AcknowledgementMessage;

/**
 * Created by Aeneas on 18.04.18.
 */
public class QRProtoSocket {
  private static final int MAX_BUFFER_SIZE = /*2953*/50; // TODO: find correct max buffer size
  private static final int SENDER_SLEEP_TIME = 10, RECEIVER_SLEEP_TIME = 10, DISPLAY_TIME = 200;

  private volatile boolean connecting = false, connected = false, canSend = true;
  private volatile int currentSequenceNumber = 1;
  private LinkedList<Message> messageQueue;
  private LinkedList<QRCode> sentQRCodes, priorityQueue;
  private LinkedList<Integer> acksToSend;
  private String remainingContent = "";
  private QRProtoPanel panel;
  private AbstractAction connectedCallback = null;
  private Webcam webcam;
  private Thread senderThread, receiverThread;

  public QRProtoSocket(int panelSize, Dimension cameraResolution) {
    messageQueue = new LinkedList<>();
    sentQRCodes = new LinkedList<>();
    priorityQueue = new LinkedList<>();

    panel = new QRProtoPanel(panelSize);

    webcam = Webcam.getWebcams().get(0);
    webcam.setViewSize(cameraResolution);

    senderThread = new Thread(new QRProtoSocketSender());
    senderThread.start();

    receiverThread = new Thread(new QRProtoSocketReceiver());
    receiverThread.start();
  }

  public void sendMessage(String message) {
    messageQueue.add(new Message(message, true));
  }

  public void connect(AbstractAction callback) {
    if(connected) {
      System.err.println("Already connected.");
      return;
    }

    System.out.println("Connecting...");

    connectedCallback = callback;
    connecting = true;

    priorityQueue.add(QRCode.SYN);
  }

  public void setConnectedCallback(AbstractAction connectedCallback) {
    this.connectedCallback = connectedCallback;
  }

  public void disconnect() {
    if(!connected) {
      System.err.println("Not connected.");
      return;
    }

    priorityQueue.add(QRCode.FIN);
    disconnected();
  }

  private void disconnected() {
    connected = false;
    connecting = false;
    synchronized(this) {
      canSend = true;
    }
    messageQueue = new LinkedList<>();
    sentQRCodes = new LinkedList<>();
    acksToSend = new LinkedList<>();

    System.out.println("Disconnected.");
  }

  private void parseMessage(Message message, int sequenceNumber, boolean acknowledgementMessage) {
    String content = message.getMessage();
    int contentLength = content.length();

    if(!connected && !connecting) {
      if(contentLength == 6 && content.substring(0, 6).equals("\\m SYN")) { // connection is being established
        priorityQueue.add(QRCode.SCK);

        connecting = true;
        synchronized(this) {
          canSend = true;
        }
      }
    } else if(connecting) {
      if(contentLength == 6 && content.substring(0, 2).equals("\\m")) { // connection message
        String msg = content.substring(3, 6);

        if(msg.equals("SCK")) { // reply with ack
          priorityQueue.add(QRCode.ACK);

          if(connectedCallback != null)
            connectedCallback.actionPerformed(new ActionEvent(this, 0, "connected")); // TODO: can the action event be composed of more useful information?

          connecting = false;
          connected = true;
          synchronized(this) {
            canSend = true;
          }

          System.out.println("Connected.");
        } else if(msg.equals("ACK")) { // connection has been established
          connecting = false;
          connected = true;
          synchronized(this) {
            canSend = true;
          }

          System.out.println("Connected.");
        }
      }
    } else if(contentLength >= 6 && content.substring(0, 2).equals("\\m")) { // socket message
        String msg = content.substring(3, 6);

        if(msg.equals("ACK")) {
          LinkedList<Integer> handleACK = new LinkedList<>();
          for (int i = 6; i < contentLength; i+=9)
            handleACK.add(ByteBuffer.wrap(Base64.getDecoder().decode(content.substring(i+1, i+9))).getInt());

          while (!handleACK.isEmpty()){
            if(handleACK.pop().equals(sentQRCodes.peek().getSequenceNumber())){
              sentQRCodes.pop();
            } else {
              priorityQueue.addAll(sentQRCodes);
              System.err.println("Messages have been resent!");
              break;
            }
          }
          if(sentQRCodes.isEmpty()) {
            synchronized(this) {
              canSend = true;
            }
          }
        } else if(msg.equals("FIN")) {
          disconnected();
        }
    } else { // content message
      System.out.println("Received content message:\n" + content);
      acksToSend.add(sequenceNumber);
      if (!acknowledgementMessage){
        String acks = "\\m ACK";
        for(int ack: acksToSend){
          acks += " ";
          acks += Base64.getEncoder().encodeToString(ByteBuffer.allocate(4).putInt(ack).array()); // adds 8 characters
        }
        priorityQueue.add(new QRCode(0, new Message(acks, true)));
        synchronized(this) {
          canSend = true;
        }
      }
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
      ArrayList<Message> messages = new ArrayList<>();
      int remainingBufferSize = MAX_BUFFER_SIZE;
      long lastTime = 0;

      //noinspection InfiniteLoopStatement
      do {
        try {
          Thread.sleep(SENDER_SLEEP_TIME);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        AcknowledgementMessage acknowledgementMessage = AcknowledgementMessage.END;

        synchronized(this) {
          if(!messageQueue.isEmpty()) {
            int length;

            while(!messageQueue.isEmpty() && (length = messageQueue.peek().getMessage().length()) < remainingBufferSize) {
              messages.add(messageQueue.pop());
              remainingBufferSize -= length;
            }
            if(!messageQueue.isEmpty() && remainingBufferSize > 0) {
              messages.add(new Message(messageQueue.peek().removeSubstring(0, remainingBufferSize), false));
              acknowledgementMessage = AcknowledgementMessage.CONTINUE;
            }

            if(lastTime == 0)
              lastTime = System.currentTimeMillis();
          }
        }

        if(canSend && !priorityQueue.isEmpty()) {
          System.out.println("Sending priority qr code with messages:");
          for(Message message: priorityQueue.peek().getMessages())
            System.out.println(message.getMessage());

          sendCode(priorityQueue.pop());

          synchronized(this) {
            if(acknowledgementMessage.equals(AcknowledgementMessage.END))
              canSend = false;
          }
        } else if(canSend && !messages.isEmpty()) {
          if(remainingBufferSize <= 0 || (System.currentTimeMillis() - lastTime) >= DISPLAY_TIME) {
            System.out.println("Sending qr code with sequence number " + currentSequenceNumber + " and messages:");
            for(Message message: messages)
              System.out.println(message.getMessage());

            sendCode(new QRCode(currentSequenceNumber, messages, acknowledgementMessage));

            synchronized(this) {
              currentSequenceNumber++;
              if(acknowledgementMessage.equals(AcknowledgementMessage.END))
                canSend = false;
            }

            remainingBufferSize = MAX_BUFFER_SIZE;
            lastTime = 0;
          }
        }
      } while(true);
    }

    private void sendCode(QRCode qrCode) {
      panel.displayQRCode(qrCode);
      sentQRCodes.add(qrCode);

      if(qrCode.getAcknowledgementMessage().equals(AcknowledgementMessage.CONTINUE)) {
        try {
          Thread.sleep(DISPLAY_TIME);
        } catch(InterruptedException e) {
          e.printStackTrace();
        }
      }
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
          boolean acknowledgementMessage = content.substring(contentLength - 6, contentLength - 4).equals(
              AcknowledgementMessage.CONTINUE); // TODO: \e is currently ignored
          byte checksum = Base64.getDecoder().decode(content.substring(contentLength - 4))[0];

          if(checksum != QRCode.checksum(content.substring(0, contentLength - 4))) {
            System.err.println("Error: Checksum not identical!");
            continue; //not necessary to handle since wrong checksum are never acknowledged
          }

          if(sequenceNumber != 0) {
            if(sequenceNumber != currentSequenceNumber + 1)
              continue; //not necessary to handle since wrong sequenceNumber are never acknowledged
            else {
              synchronized(this) {
                currentSequenceNumber++;
              }
            }
          }

          content = remainingContent + content.substring(8, contentLength - 6); // concat the remaining content from the last message
          contentLength = content.length();

          Vector<Message> messages = new Vector<>();
          int current = 0, next;
          while((next = content.indexOf(Message.MESSAGE_END, current)) != -1) {
            next += Message.MESSAGE_END.length();
            messages.add(new Message(content.substring(current, next), true));
            current = next;
          }
          remainingContent = content.substring(current, contentLength); // this is the remaining content that is not a complete message

          for(Message message: messages)
            parseMessage(message.unescape(), sequenceNumber, acknowledgementMessage);
        }
      } while(true);
    }
  }
}
