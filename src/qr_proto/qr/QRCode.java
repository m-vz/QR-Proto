package qr_proto.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import qr_proto.Message;

public class QRCode {
  public static final QRCode
      SYN = new QRCode(0, new Message("\\m SYN", true).escape()),
      ACK = new QRCode(0, new Message("\\m ACK", true).escape()),
      SCK = new QRCode(0, new Message("\\m SCK", true).escape()),
      FIN = new QRCode(0, new Message("\\m FIN", true).escape());
  private ArrayList<Message> messages = new ArrayList<>();

  private static QRCodeWriter qrCodeWriter = new QRCodeWriter();
  private static Map<EncodeHintType, Object> hintMap = new EnumMap<>(EncodeHintType.class);
  static {
    hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    hintMap.put(EncodeHintType.MARGIN, 0); // defaults to 4
    hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
  }

  private int sequenceNumber;
  public QRCode(int sequenceNumber, ArrayList<Message> messages){
    this (sequenceNumber, messages, AcknowledgementMessage.END);
  }
  private AcknowledgementMessage acknowledgementMessage;

  public QRCode(int sequenceNumber, ArrayList<Message> messages, AcknowledgementMessage acknowledgementMessage) {
    this.sequenceNumber = sequenceNumber;
    this.messages.addAll(messages);
    this.acknowledgementMessage = acknowledgementMessage;
  }

  public QRCode(int sequenceNumber, Message message){
    this (sequenceNumber, message, AcknowledgementMessage.END);
  }

  public QRCode(int sequenceNumber, Message message, AcknowledgementMessage acknowledgementMessage) {
    this.sequenceNumber = sequenceNumber;
    this.messages.add(message);
    this.acknowledgementMessage = acknowledgementMessage;
  }

  public BitMatrix generateBitMatrix() {
    StringBuilder qrMessage = new StringBuilder();

    qrMessage.append(Base64.getEncoder().encodeToString(ByteBuffer.allocate(4).putInt(sequenceNumber).array())); // adds 8 characters
    for(Message message: messages)
      qrMessage.append(message.escape());
    qrMessage.append(acknowledgementMessage); // adds 2 characters
    qrMessage.append(Base64.getEncoder().encodeToString(new byte[]{checksum(qrMessage.toString())})); // adds 4 characters

    try {
      return qrCodeWriter.encode(qrMessage.toString(), BarcodeFormat.QR_CODE, 0, 0, hintMap);
    } catch(WriterException e) {
      e.printStackTrace();
      return null;
    }
  }

  public ArrayList<Message> getMessages() {
    return messages;
  }

  public static byte checksum(String _message) {
    byte checksum = Byte.MIN_VALUE;
    int length = _message.length();
    for (int i = 0; i < length; i++) {
      if (i % 2 == 0) {
        checksum += 3 *  _message.charAt(i);
      } else {
        checksum += _message.charAt(i);
      }
    }
    return checksum;
  }

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public enum AcknowledgementMessage{
    END(false), CONTINUE(true);

    private final boolean cont;

    AcknowledgementMessage(boolean cont) {
      this.cont = cont;
    }

    @Override
    public String toString() {
      if (cont) return "\\c";
      else return "\\e";
    }
  }

  public AcknowledgementMessage getAcknowledgementMessage() {
    return acknowledgementMessage;
  }
}
