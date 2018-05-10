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
class QRProtoSocket {
  private static final int MAX_BUFFER_SIZE = (2953 - QRCode.METADATA_LENGTH - 1)/8; // don't ask
  private static final int SENDER_SLEEP_TIME = 10, RECEIVER_SLEEP_TIME = 10, DISPLAY_TIME = 400;

  private volatile boolean connecting, connected, canSend, sentERR = false;
  private volatile int currentSequenceNumber, currentSequenceNumberOffset;
  private volatile String received;
  private LinkedList<Message> messageQueue;
  private LinkedList<QRCode> sentQRCodes, priorityQueue, errorQueue;
  private QRCode ackToSend;
  private QRProtoPanel panel;
  private AbstractAction connectedCallback, connectingCallback, disconnectedCallback, canSendCallback, receivedCallback, errorCallback;
  private Webcam webcam;
  private Dimension webcamResolution;
  private QRProtoSocketSender sender;
  private QRProtoSocketReceiver receiver;

  QRProtoSocket(int panelSize, Dimension cameraResolution) {
    panel = new QRProtoPanel(panelSize);

    webcamResolution = cameraResolution;
    webcam = Webcam.getWebcams().get(0);
    webcam.setCustomViewSizes(cameraResolution);
    webcam.setViewSize(cameraResolution);
    if(!webcam.isOpen())
      webcam.open();

    init();
  }

  void init() {
    connecting = false;
    connected = false;
    canSend = true;
    currentSequenceNumber = 0;
    currentSequenceNumberOffset = 0;
    received = "";
    ackToSend = null;
    connectedCallback = null;
    connectingCallback = null;
    disconnectedCallback = null;
    canSendCallback = null;
    receivedCallback = null;
    errorCallback = null;
    messageQueue = new LinkedList<>();
    sentQRCodes = new LinkedList<>();
    priorityQueue = new LinkedList<>();
    errorQueue = new LinkedList<>();

    if(sender != null)
      sender.shouldEnd = true;
    sender = new QRProtoSocketSender();
    new Thread(sender).start();

    if(receiver != null)
      receiver.shouldEnd = true;
    receiver = new QRProtoSocketReceiver();
    new Thread(receiver).start();

    panel.displayNothing();
  }

  void sendMessage(String message) {
    messageQueue.add(new Message(message, true).escape());
  }

  void connect() {
    if(connected) {
      Log.errln("Already connected.");
      return;
    }

    Log.outln("Connecting...");

    this.connecting = true;

    priorityQueue.add(new QRCode(QRCode.QRCodeType.SYN));
  }

  void disconnect() {
    if(!connected) {
      Log.errln("Not connected.");
      return;
    }

    disconnected();
    priorityQueue.add(new QRCode(QRCodeType.FIN));
  }

  void end() {
    sender.shouldEnd = true;
    receiver.shouldEnd = true;
    webcam.close();
  }

  private void disconnected() {
    init();

    if(disconnectedCallback != null)
      disconnectedCallback.actionPerformed(new ActionEvent(this, 0, "disconnected"));

    Log.outln("Disconnected.");
  }

  void setConnectedCallback(AbstractAction connectedCallback) {
    this.connectedCallback = connectedCallback;
  }

  void setConnectingCallback(AbstractAction connectingCallback) {
    this.connectingCallback = connectingCallback;
  }

  void setDisconnectedCallback(AbstractAction disconnectedCallback) {
    this.disconnectedCallback = disconnectedCallback;
  }

  void setCanSendCallback(AbstractAction canSendCallback) {
    this.canSendCallback = canSendCallback;
  }

  void setReceivedCallback(AbstractAction receivedCallback) {
    this.receivedCallback = receivedCallback;
  }

  void setErrorCallback(AbstractAction errorCallback) {
    this.errorCallback = errorCallback;
  }

  String getReceived() {
    return received;
  }

  QRProtoPanel getPanel() {
    return panel;
  }

  Dimension getWebcamResolution() {
    return webcamResolution;
  }

  private class QRProtoSocketSender implements Runnable {
    boolean shouldEnd = false;

    @Override
    public void run() {
      ArrayList<Message> messages = new ArrayList<>();
      AcknowledgementMessage acknowledgementMessage = AcknowledgementMessage.END;
      int remainingBufferSize = MAX_BUFFER_SIZE;
      long lastTime = 0;

      //noinspection InfiniteLoopStatement
      try {
        do {
          try {
            Thread.sleep(SENDER_SLEEP_TIME);
          } catch(InterruptedException e) {
            e.printStackTrace();
          }

          synchronized(this) {
            if(!messageQueue.isEmpty()) {
              int length;

              while(!messageQueue.isEmpty() && (length = Objects.requireNonNull(messageQueue.peek()).getMessage().length()) < remainingBufferSize) {
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

            if(ackToSend != null) {
              priorityQueue.add(ackToSend);

              ackToSend = null;
            }

            if(canSend) {
              QRCode code;

              if(!priorityQueue.isEmpty()) {
                code = priorityQueue.pop();

                if(code.getType().equals(QRCodeType.MSG))
                  throw new Exception("Priority queue cannot contain codes of type MSG.");

                if(code.getType().equals(QRCodeType.SYN) || // SYNs need to wait for SCK
                    code.getType().equals(QRCodeType.SCK)) // SCKs need to wait for ACK
                  canSend = false;

                switch(code.getType()) {
                  case SYN:
                  case SCK:
                    currentSequenceNumberOffset++;
                    break;
                  case ERR:
                    break;
                  case ACK:
                  case FIN:
                    currentSequenceNumber++;
                    break;
                }
                code.setSequenceNumber(currentSequenceNumber + currentSequenceNumberOffset);

                Log.outln("Sending priority qr code:");
                Log.outln(code);

                for(Message message: code.getMessages())
                  message.escape();
                sendCode(code);
              } else if(!errorQueue.isEmpty()) {
                code = errorQueue.pop();

                if(!code.getType().equals(QRCodeType.MSG))
                  throw new Exception("Error queue can only contain codes of type MSG, but not " + code.getType() + ".");

                if(code.getAcknowledgementMessage().equals(AcknowledgementMessage.END)) // MSGs need to wait for ACK if END
                  canSend = false;
                currentSequenceNumberOffset++;

                Log.outln("Sending error qr code:");
                Log.outln(code);

                for(Message message: code.getMessages())
                  message.escape();
                sendCode(code);
              } else if(!messages.isEmpty()) {
                if(remainingBufferSize <= 0 || (System.currentTimeMillis() - lastTime) >= DISPLAY_TIME) {
                  code = new QRCode(messages, acknowledgementMessage);

                  if(!code.getType().equals(QRCodeType.MSG))
                    throw new Exception("Message queue can only contain codes of type MSG, but not " + code.getType() + ".");

                  if(acknowledgementMessage.equals(AcknowledgementMessage.END))
                    canSend = false;
                  currentSequenceNumberOffset++;

                  code.setSequenceNumber(currentSequenceNumber + currentSequenceNumberOffset);

                  Log.outln("Sending qr code:");
                  Log.outln(code);

                  messages.clear();
                  remainingBufferSize = MAX_BUFFER_SIZE;
                  lastTime = 0;

                  sendCode(code);
                }
              }
            }
          }
        } while(!shouldEnd);

        Log.outln("Sending thread stopped.");
      } catch(Exception e) {
        e.printStackTrace();
        Log.errln("Stopping sending thread.");
      }
    }

    private void sendCode(QRCode qrCode) {
      sentERR = false;
      if(qrCode.getType().equals(QRCodeType.MSG))
        sentQRCodes.add(qrCode);
      else if(qrCode.getType().equals(QRCodeType.ERR))
        sentERR = true;

      panel.displayQRCode(qrCode);

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
    private Vector<Message> messages = new Vector<>();
    private String remainingContent = "";
    boolean shouldEnd = false;

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
            Log.errln("Error: Checksum not identical!");
            continue; // not necessary to handle since wrong checksum are never acknowledged
          }

          if(!type.equals(QRCodeType.ERR)) {
            if(sequenceNumber <= currentSequenceNumber + currentSequenceNumberOffset) { // a message has been read twice
              continue; // ignore all messages that have been read before
            } else if(sequenceNumber > currentSequenceNumber + currentSequenceNumberOffset + 1) { // a message has been lost
              Log.errln("Received code with incorrect sequence number " + sequenceNumber + ".");
              if(type.equals(QRCodeType.MSG)) {
                Log.errln("Sending ERR for sequence number " + currentSequenceNumber + " (current offset is " + currentSequenceNumberOffset + ").");
                synchronized(this) {
                  ackToSend = new QRCode(currentSequenceNumber, true);

                  if(errorCallback != null)
                    errorCallback.actionPerformed(new ActionEvent(this, 0, "error"));
                }
              }
              continue;
            } else {
              synchronized(this) {
                ackToSend = null;
              }
            }

            if(sentERR)
              panel.displayNothing();
          }

          String content = remainingContent + rawContent.substring(HEADER_SIZE, rawContentLength - CHECKSUM_SIZE); // concat the remaining content from the last message

          int current = 0, next;
          while((next = content.indexOf(Message.MESSAGE_END, current)) != -1) {
            next += Message.MESSAGE_END.length();
            messages.add(new Message(content.substring(current, next), true, true));
            current = next;
          }

          remainingContent = content.substring(current); // this is the remaining content that is not a complete message
          Log.outln("Received code with type " + type + ", sequence number " + sequenceNumber + (content.length() > 0 ? " and content: " + content : " without any content."));

          if(acknowledgementMessage.equals(AcknowledgementMessage.END)) {
            if(remainingContent.length() == 0 && messages.isEmpty()) {
              Log.outln("Received code with type " + type + " and sequence number " + sequenceNumber);
              parseMessage(new Message("", true), type, sequenceNumber);
            } else {
              for (Message message : messages)
                parseMessage(message.unescape(), type, sequenceNumber);
              messages = new Vector<>();
            }
          } else {
            synchronized(this) {
              currentSequenceNumber++;
            }
          }
        }
      } while(!shouldEnd);

      Log.outln("Receiving thread stopped.");
    }

    private void parseMessage(Message message, QRCodeType type, int sequenceNumber) {
      String content = message.getMessage();
      int contentLength = content.length();

      if(connected) {
        switch(type) {
          case MSG: // content message
            synchronized (this) {
              currentSequenceNumber++;
              ackToSend = new QRCode(currentSequenceNumber, false);
            }

            Log.outln("Received content message:\n" + content);
            received = content;

            if(receivedCallback != null)
              receivedCallback.actionPerformed(new ActionEvent(this, 0, "received"));
            break;
          case ACK:
            if(contentLength != 8) {
              Log.errln("Received invalid ACK, ignoring.");
              break;
            }

            synchronized(this) {
              Integer acknowledgedSequenceNumber = ByteBuffer.wrap(Base64.getDecoder().decode(content.substring(0, 8))).getInt();
              currentSequenceNumber = acknowledgedSequenceNumber + 1;
              currentSequenceNumberOffset = 0;
              canSend = true;
            }

            sentQRCodes.clear();

            if(canSendCallback != null)
              canSendCallback.actionPerformed(new ActionEvent(this, 0, "can send"));
            break;
          case ERR:
            Integer acknowledgedSequenceNumber = ByteBuffer.wrap(Base64.getDecoder().decode(content.substring(0, 8))).getInt();

            synchronized(this) {
              sentQRCodes.addAll(errorQueue);
              errorQueue.clear();

              currentSequenceNumber = acknowledgedSequenceNumber;
              currentSequenceNumberOffset = 0;
              canSend = true;

              sentQRCodes.removeIf(o -> o.getSequenceNumber() <= acknowledgedSequenceNumber);

              Log.errln("Messages have been resent from sequence number " + (acknowledgedSequenceNumber + 1) + " (a total of " + sentQRCodes.size() + " codes).");

              errorQueue.addAll(sentQRCodes);
              sentQRCodes.clear();
            }

            if(errorCallback != null)
              errorCallback.actionPerformed(new ActionEvent(this, 0, "error"));
            break;
          case FIN:
            disconnected();
            break;
        }
      } else { // TODO: maybe handle ERR cases as well?
        if(connecting) {
          switch(type) {
            case SCK: // reply with ack
              synchronized(this) {
                currentSequenceNumber = currentSequenceNumber + currentSequenceNumberOffset + 1;
                currentSequenceNumberOffset = 0;
              }

              priorityQueue.add(new QRCode(QRCodeType.ACK));

              connecting = false;
              connected = true;
              synchronized(this) {
                canSend = true;
              }

              Log.outln("Connected.");

              if(connectedCallback != null)
                connectedCallback.actionPerformed(new ActionEvent(this, 0, "connected"));
              if(canSendCallback != null)
                canSendCallback.actionPerformed(new ActionEvent(this, 0, "can send"));
              break;
            case ACK: // connection has been established
              synchronized(this) {
                currentSequenceNumber = currentSequenceNumber + currentSequenceNumberOffset + 1;
                currentSequenceNumberOffset = 0;
              }

              connecting = false;
              connected = true;
              synchronized(this) {
                canSend = true;
              }

              Log.outln("Connected.");

              if(connectedCallback != null)
                connectedCallback.actionPerformed(new ActionEvent(this, 0, "connected"));
              if(canSendCallback != null)
                canSendCallback.actionPerformed(new ActionEvent(this, 0, "can send"));
              break;
          }
        } else {
          if(type.equals(QRCodeType.SYN)) {
            synchronized(this) {
              currentSequenceNumber++;
            }

            priorityQueue.add(new QRCode(QRCodeType.SCK));

            if(connectingCallback != null)
              connectingCallback.actionPerformed(new ActionEvent(this, 0, "connecting"));

            connecting = true;
            synchronized(this) {
              canSend = true;
            }
          }
        }
      }
    }
  }
}
