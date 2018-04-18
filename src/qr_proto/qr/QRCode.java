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
    CONTINUE, END, END_OF_ACK
  }

  public static final QRCode
      SYN = new QRCode(0, new Message("\\m SYN")),
      ACK = new QRCode(0, new Message("\\m ACK")),
      SCK = new QRCode(0, new Message("\\m SCK")),
      FIN = new QRCode(0, new Message("\\m FIN"));

  private BitMatrix bitMatrix;

  public  QRCode(int sequenceNumber, Message content){
    this (sequenceNumber, content, AcknowledgementMessage.CONTINUE);
  }

  public QRCode(int sequenceNumber, Message content, AcknowledgementMessage ackmessage) {
    String qrmessage = "";
    int size = 250; // TODO: automatic size calculation.

    Map<EncodeHintType, Object> hintMap = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
    hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    hintMap.put(EncodeHintType.MARGIN, 1); // defaults to 4
    hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

    qrmessage += Base64.getEncoder().encodeToString(ByteBuffer.allocate(4).putInt(sequenceNumber).array());
    qrmessage += content;
    switch (ackmessage) {
      case CONTINUE:
        qrmessage += "\\c";
        break;
      case END:
        qrmessage += "\\e";
        break;
      case END_OF_ACK:
        qrmessage += "\\a";
        break;
    }
    qrmessage += Base64.getEncoder().encodeToString(new byte[]{checksum(content.getMessage())});

    try {
      bitMatrix = (new QRCodeWriter()).encode(qrmessage, BarcodeFormat.QR_CODE, size, size, hintMap);
    } catch(WriterException e) {
      e.printStackTrace();
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

  public BitMatrix getBitMatrix() {
    return bitMatrix;
  }
}
