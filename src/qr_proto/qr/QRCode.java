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
  private static QRCodeWriter qrCodeWriter = new QRCodeWriter();
  private static Map<EncodeHintType, Object> hintMap = new EnumMap<>(EncodeHintType.class);
  static {
    hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    hintMap.put(EncodeHintType.MARGIN, 0); // defaults to 4
    hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
  }

  private int sequenceNumber = -1;
  private QRCodeType type;
  private AcknowledgementMessage acknowledgementMessage;
  private ArrayList<Message> messages = new ArrayList<>();

  public QRCode(ArrayList<Message> messages, AcknowledgementMessage acknowledgementMessage) {
    this.type = QRCodeType.MSG;
    this.acknowledgementMessage = acknowledgementMessage;
    this.messages.addAll(messages);
  }

  public QRCode(ArrayList<Message> messages) {
    this(messages, AcknowledgementMessage.END);
  }

  public QRCode(Message message, AcknowledgementMessage acknowledgementMessage) {
    this.type = QRCodeType.MSG;
    this.acknowledgementMessage = acknowledgementMessage;
    this.messages.add(message);
  }

  public QRCode(Message message) {
    this(message, AcknowledgementMessage.END);
  }

  public QRCode(int sequenceNumberToAcknowledge) {
    this.type = QRCodeType.ACK;
    this.acknowledgementMessage = AcknowledgementMessage.END;
    this.messages.add(new Message(Base64.getEncoder().encodeToString(ByteBuffer.allocate(4).putInt(sequenceNumberToAcknowledge).array()), true));
  }

  public QRCode(QRCodeType type) {
    if(type.equals(QRCodeType.MSG))
      throw new IllegalArgumentException("Primitive qr codes cannot have type MSG.");

    this.type = type;
    this.acknowledgementMessage = AcknowledgementMessage.END;
  }

  public BitMatrix generateBitMatrix() {
    // TODO: throw if sequence number is -1

    StringBuilder qrMessage = new StringBuilder();

    ByteBuffer header = ByteBuffer.allocate(6);
    header.putInt(sequenceNumber);
    header.put(type.getCode());
    header.put(acknowledgementMessage.toByte());

    qrMessage.append(Base64.getEncoder().encodeToString(header.array())); // adds 8 characters
    for(Message message: messages)
      qrMessage.append(message.escape());
    qrMessage.append(Base64.getEncoder().encodeToString(new byte[]{checksum(qrMessage.toString())})); // adds 4 characters

    try {
      return qrCodeWriter.encode(qrMessage.toString(), BarcodeFormat.QR_CODE, 0, 0, hintMap);
    } catch(WriterException e) {
      e.printStackTrace();
      return null;
    }
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

  public void setSequenceNumber(int sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
  }

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public QRCodeType getType() {
    return type;
  }

  public AcknowledgementMessage getAcknowledgementMessage() {
    return acknowledgementMessage;
  }

  public ArrayList<Message> getMessages() {
    return messages;
  }

  @Override
  public boolean equals(Object obj) {
    return obj.getClass().equals(QRCode.class) && sequenceNumber == ((QRCode) obj).getSequenceNumber();
  }

  public enum AcknowledgementMessage{
    END(false), CONTINUE(true);

    private final boolean cont;

    AcknowledgementMessage(boolean cont) {
      this.cont = cont;
    }

    byte toByte() {
      return cont ? (byte) 1 /* ~= CONTINUE */ : (byte) 0 /* ~= END */;
    }
  }

  public enum QRCodeType {
    SYN(0), ACK(1), SCK(2), FIN(3), MSG(4);

    private final byte code;

    QRCodeType(int code) {
      this.code = (byte) code;
    }



    public static QRCodeType fromByte(byte i) {
      switch(i) {
        case 0:
          return SYN;
        case 1:
          return ACK;
        case 2:
          return SCK;
        case 3:
          return FIN;
        default:
          return MSG;
      }
    }

    byte getCode() {
      return code;
    }

    @Override
    public String toString() {
      switch(code) {
        case 0:
          return "SYN";
        case 1:
          return "ACK";
        case 2:
          return "SCK";
        case 3:
          return "FIN";
        case 4:
          return "MSG";
        default:
          return "INVALID";
      }
    }
  }
}
