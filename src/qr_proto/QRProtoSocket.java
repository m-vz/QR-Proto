package qr_proto;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.*;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import qr_proto.gui.QRProtoPanel;
import qr_proto.qr.QRCode;

import javax.swing.*;
import qr_proto.qr.QRCode.AcknowledgementMessage;
import qr_proto.qr.QRCode.QRCodeType;

/**
 * Created by Aeneas on 18.04.18.
 */
public class QRProtoSocket {
  private static final int MAX_BUFFER_SIZE = /*2953*/50; // TODO: find correct max buffer size
  private static final int SENDER_SLEEP_TIME = 10, RECEIVER_SLEEP_TIME = 10, DISPLAY_TIME = 200;

  private volatile boolean connecting = false, connected = false, canSend = true;
  private volatile int currentSequenceNumber = 0;
  private LinkedList<Message> messageQueue;
  private LinkedList<QRCode> sentQRCodes, priorityQueue;
  private int ackToSend = -1;
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

    priorityQueue.add(new QRCode(QRCode.QRCodeType.SYN));
  }

  public void setConnectedCallback(AbstractAction connectedCallback) {
    this.connectedCallback = connectedCallback;
  }

  public void disconnect() {
    if(!connected) {
      System.err.println("Not connected.");
      return;
    }

    disconnected();
    priorityQueue.add(new QRCode(QRCodeType.FIN)); // FIXME: canSend will never be true again after FIN was sent.
  }

  private void disconnected() {
    connected = false;
    connecting = false;
    synchronized(this) {
      currentSequenceNumber = 0;
      canSend = true;
    }
    messageQueue = new LinkedList<>();
    sentQRCodes = new LinkedList<>();
    priorityQueue = new LinkedList<>();
    ackToSend = -1;

    System.out.println("Disconnected.");
  }

  private void parseMessage(Message message, QRCodeType type, int sequenceNumber) {
    String content = message.getMessage();
    int contentLength = content.length();

    if(connected) {
      switch(type) {
        case MSG: // content message
          synchronized (this) {
            ackToSend = sequenceNumber;
          }

          System.out.println("Received content message:\n" + content);
          break;
        case ACK:
          if(contentLength != 8) {
            System.err.println("Received invalid ACK, ignoring.");
            break;
          }

          Integer acknowledgedSequenceNumber = ByteBuffer.wrap(Base64.getDecoder().decode(content.substring(0, 8))).getInt();

          sentQRCodes.removeIf(o -> o.getSequenceNumber() <= acknowledgedSequenceNumber);

          if(sentQRCodes.isEmpty()) {
            synchronized(this) {
              canSend = true;
            }
          } else {
            System.err.println("Messages have been resent from sequence number " + (acknowledgedSequenceNumber + 1));

            priorityQueue.addAll(sentQRCodes);
          }
          break;
        case FIN:
          disconnected();
          break;
      }
    } else {
      if(connecting) {
        switch(type) {
          case SCK: // reply with ack
            priorityQueue.add(new QRCode(QRCodeType.ACK));

            if(connectedCallback != null)
              connectedCallback.actionPerformed(new ActionEvent(this, 0, "connected")); // TODO: can the action event be composed of more useful information?

            connecting = false;
            connected = true;
            synchronized(this) {
              canSend = true;
            }

            System.out.println("Connected.");
            break;
          case ACK: // connection has been established
            connecting = false;
            connected = true;
            synchronized(this) {
              canSend = true;
            }

            System.out.println("Connected.");
            break;
        }
      } else {
        if(type.equals(QRCodeType.SYN)) {
          priorityQueue.add(new QRCode(QRCodeType.SCK));

          connecting = true;
          synchronized(this) {
            canSend = true;
          }
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
      AcknowledgementMessage acknowledgementMessage = AcknowledgementMessage.END;
      int remainingBufferSize = MAX_BUFFER_SIZE;
      long lastTime = 0;

      //noinspection InfiniteLoopStatement
      do {
        try {
          Thread.sleep(SENDER_SLEEP_TIME);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        synchronized(this) {
          if(!messageQueue.isEmpty()) {
            int length;

            while(!messageQueue.isEmpty() && (length = messageQueue.peek().getMessage().length()) < remainingBufferSize) {
              messages.add(messageQueue.pop());
              acknowledgementMessage = AcknowledgementMessage.END;
              remainingBufferSize -= length;
            }
            if(!messageQueue.isEmpty() && remainingBufferSize > 0) {
              messages.add(new Message(messageQueue.peek().removeSubstring(0, remainingBufferSize), false));
              acknowledgementMessage = AcknowledgementMessage.CONTINUE;
              remainingBufferSize = 0;
            }

            if(lastTime == 0)
              lastTime = System.currentTimeMillis();
          }
        }

        synchronized (this) {
          if(ackToSend >= 0) {
            System.out.println("Adding ack with number " + ackToSend + " to priority queue.");
            priorityQueue.add(new QRCode(ackToSend));

            canSend = true;
            ackToSend = -1;
          }
        }

        if(canSend) {
          if(!priorityQueue.isEmpty()) {
            QRCode code = priorityQueue.pop();

            synchronized(this) {
              if(code.getType().equals(QRCodeType.SYN) || // SYNs need to wait for SCK
                  code.getType().equals(QRCodeType.MSG) && code.getAcknowledgementMessage().equals(AcknowledgementMessage.END)) // MSGs need to wait for ACK if END
                canSend = false;

              currentSequenceNumber++;
            }

            code.setSequenceNumber(currentSequenceNumber);

            System.out.println("Sending priority qr code with sequence number " + code.getSequenceNumber() + " and messages:");
            for(Message message: code.getMessages())
              System.out.println(message.getMessage());

            sendCode(code);
          } else if(!messages.isEmpty()) {
            if(remainingBufferSize <= 0 || (System.currentTimeMillis() - lastTime) >= DISPLAY_TIME) {
              QRCode code = new QRCode(messages, acknowledgementMessage);

              synchronized(this) {
                if(acknowledgementMessage.equals(AcknowledgementMessage.END))
                  canSend = false;

                currentSequenceNumber++;
              }

              code.setSequenceNumber(currentSequenceNumber);

              System.out.println("Sending qr code with sequence number " + currentSequenceNumber + " and messages:");
              for(Message message: messages)
                System.out.println(message.getMessage());

              sendCode(code);

              messages.clear();
              remainingBufferSize = MAX_BUFFER_SIZE;
              lastTime = 0;
            }
          }
        }
      } while(true);
    }

    private void sendCode(QRCode qrCode) {
      panel.displayQRCode(qrCode);
      if(qrCode.getType().equals(QRCodeType.MSG))
        sentQRCodes.add(qrCode);

      if(canSend) {
        try {
          Thread.sleep(DISPLAY_TIME);
        } catch(InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  class QRProtoSocketReceiver implements Runnable {
    private static final int HEADER_SIZE = 8, CHECKSUM_SIZE = 4;

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
          String rawContent = result.getText();
          int rawContentLength = rawContent.length();

          byte[] header = Base64.getDecoder().decode(rawContent.substring(0, HEADER_SIZE));
          int sequenceNumber = ByteBuffer.wrap(new byte[]{header[0], header[1], header[2], header[3]}).getInt();
          QRCodeType type = QRCodeType.fromByte(header[4]);
          AcknowledgementMessage acknowledgementMessage = header[5] == 1 ? AcknowledgementMessage.CONTINUE : AcknowledgementMessage.END;
          byte checksum = Base64.getDecoder().decode(rawContent.substring(rawContentLength - CHECKSUM_SIZE))[0];

          if(checksum != QRCode.checksum(rawContent.substring(0, rawContentLength - CHECKSUM_SIZE))) {
            System.err.println("Error: Checksum not identical!");
            continue; // not necessary to handle since wrong checksum are never acknowledged
          }

          if(sequenceNumber != currentSequenceNumber + 1) {
            if(type.equals(QRCodeType.MSG))
              ackToSend = currentSequenceNumber; // TODO: this sends very many acks, maybe too many?
            continue;
          } else {
            synchronized(this) {
              currentSequenceNumber = sequenceNumber;
            }
          }

          String content = remainingContent + rawContent.substring(HEADER_SIZE, rawContentLength - CHECKSUM_SIZE); // concat the remaining content from the last message

          Vector<Message> messages = new Vector<>();
          int current = 0, next;
          while((next = content.indexOf(Message.MESSAGE_END, current)) != -1) {
            next += Message.MESSAGE_END.length();
            messages.add(new Message(content.substring(current, next), true, true));
            current = next;
          }

          remainingContent = content.substring(current); // this is the remaining content that is not a complete message
          if(remainingContent.length() > 0)
            System.out.println("Remaining content: " + remainingContent);
          if(content.length() > 0)
            System.out.println("Content: " + content);

          if(acknowledgementMessage.equals(AcknowledgementMessage.END))
            for(Message message: messages)
              parseMessage(message.unescape(), type, sequenceNumber);
          else if(remainingContent.length() == 0 && messages.isEmpty())
            parseMessage(new Message("", true), type, sequenceNumber);
        }
      } while(true);
    }
  }
}
