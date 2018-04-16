/**
 * Created by Aeneas on 14.04.18.
 * Code template used from: https://crunchify.com/java-simple-qr-code-generator-example/
 */

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.SystemColor;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class QRGenerator {

  public static int checksum(String _message) {
    int checksum = 0;
    int length = _message.length();
    for (int i = 0; i < length; i++) {
      if (i % 2 == 0) {
        checksum += 3 * (int) _message.charAt(i);
      } else {
        checksum += (int) _message.charAt(i);
      }
      //System.out.println((int)_message.charAt(i));
    }
    checksum = checksum % 255;
    checksum = 255 - checksum;
    //System.out.println("Checksum = " + checksum);
    return checksum;
  }

  public static void makeQR(String _message, String _filename) {

    String filename = _filename;
    String fileType = "png";
    String filePath =
        "/Users/Aeneas/Nextcloud/Studium/08. FS2018/Internet and Security/QR-Proto/img/" + filename
            + "."
            + fileType;
    int size = 250;

    String checkMessage = _message + ";" + Integer.toString(checksum(_message));

    File myFile = new File(filePath);
    try {

      Map<EncodeHintType, Object> hintMap = new EnumMap<EncodeHintType, Object>(
          EncodeHintType.class);
      hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");

      // Now with zxing version 3.2.1 you could change border size (white border size to just 1)
      hintMap.put(EncodeHintType.MARGIN, 1); /* default = 4 */
      hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

      QRCodeWriter qrCodeWriter = new QRCodeWriter();
      BitMatrix byteMatrix = qrCodeWriter.encode(checkMessage, BarcodeFormat.QR_CODE, size,
          size, hintMap);
      int width = byteMatrix.getWidth();
      BufferedImage image = new BufferedImage(width, width,
          BufferedImage.TYPE_INT_RGB);
      image.createGraphics();

      Graphics2D graphics = (Graphics2D) image.getGraphics();
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, width, width);
      graphics.setColor(Color.BLACK);

      for (int i = 0; i < width; i++) {
        for (int j = 0; j < width; j++) {
          if (byteMatrix.get(i, j)) {
            graphics.fillRect(i, j, 1, 1);
          }
        }
      }
      ImageIO.write(image, fileType, myFile);
    } catch (WriterException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("\n\nYou have successfully created QR Code.");
  }

  public static String checkMessage(String _message) {
    String message = _message;
    String cutMessage = message.substring(0, message.lastIndexOf(";"));
    int originalChecksum;
    int derivedChecksum;

    if (message != null) {
      originalChecksum = Integer.parseInt(message.substring(message.lastIndexOf(";") + 1));
      derivedChecksum = checksum(cutMessage);
      /*System.out.println(
          "Last index of equals: " + message.substring(message.lastIndexOf(";")) + " togehter with "
              + message.substring(0, message.lastIndexOf(";") - 1));*/
      if (originalChecksum != derivedChecksum) {
        System.out.println("Error: Checksum not identical");
        cutMessage = null;
      }
    }
    return cutMessage;

  }

}