package qr_proto.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import qr_proto.Message;

public class QRCode {
  public enum AcknowledgementMessage{
    END(false), CONTINUE(true);

    private final boolean cont;

    private AcknowledgementMessage(boolean cont) {
      this.cont = cont;
    }

    @Override
    public String toString() {
      if (cont) return "\\c";
      else return "\\e";

    }
  }

  public static final QRCode
      SYN = new QRCode(0, new Message("\\m SYN").escape()),
      ACK = new QRCode(0, new Message("\\m ACK").escape()),
      ACK_CONTINUE = new QRCode(0, new Message("\\m ACK").escape(), AcknowledgementMessage.CONTINUE),
      SCK = new QRCode(0, new Message("\\m SCK").escape()),
      FIN = new QRCode(0, new Message("\\m FIN").escape());

  private int sequenceNumber;
  private Message content;
  private AcknowledgementMessage acknowledgementMessage;

  public QRCode(int sequenceNumber, Message content){
    this (sequenceNumber, content, AcknowledgementMessage.END);
  }

  public QRCode(int sequenceNumber, Message content, AcknowledgementMessage acknowledgementMessage) {
    this.sequenceNumber = sequenceNumber;
    this.content = content;
    this.acknowledgementMessage = acknowledgementMessage;
  }

  public BitMatrix generateBitMatrix(int size) {
    String qrMessage = "";

    Map<EncodeHintType, Object> hintMap = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
    hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    hintMap.put(EncodeHintType.MARGIN, 0); // defaults to 4
    hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

    qrMessage += Base64.getEncoder().encodeToString(ByteBuffer.allocate(4).putInt(sequenceNumber).array()); // adds 8 characters
    qrMessage += content;
    qrMessage += acknowledgementMessage;

    qrMessage += Base64.getEncoder().encodeToString(new byte[]{checksum(qrMessage)}); // adds 4 characters

    try {
      return (new QRCodeWriter()).encode(qrMessage, BarcodeFormat.QR_CODE, size, size, hintMap);
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

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public Message getContent() {
    return content;
  }

  public AcknowledgementMessage getAcknowledgementMessage() {
    return acknowledgementMessage;
  }
}
