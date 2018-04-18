package qr_proto;

import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import qr_proto.gui.QRProtoPanel;
import qr_proto.qr.QRCode;
import qr_proto.qr.QRGenerator;


public class QRProto implements Runnable, ThreadFactory {
  private Webcam webcam;
  private QRProtoPanel panel;

  private String received = "";

  private boolean shouldClose = false;

  public QRProto(QRProtoPanel panel) {
    QRCode qrCode = QRGenerator.makeQR("some message\uD83D\uDE34");

    webcam = Webcam.getWebcams().get(0);

    this.panel = panel;

    Executors.newSingleThreadExecutor(this).execute(this);
  }

  @Override
  public void run() {
    do {
      try {
        Thread.sleep(100);
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

      if (result != null) {
        String checkedMessage = result.getText();

        if (checkedMessage != null)
          receivedMessage(checkedMessage);
      }
    } while(!shouldClose);
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread t = new Thread(r, "qr-proto-runner");
    t.setDaemon(true);
    return t;
  }

  private void receivedMessage(String message) {
    received = message;

    System.out.println(message);
  }

  public String getReceivedMessage() {
    return received;
  }

  QRProtoPanel getQRProtoPanel() {
    return panel;
  }

  public void shouldClose() {
    shouldClose = true;
  }
}
