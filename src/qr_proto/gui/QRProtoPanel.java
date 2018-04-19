package qr_proto.gui;

import com.google.zxing.common.BitMatrix;
import qr_proto.qr.QRCode;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class QRProtoPanel extends JPanel {
  private BufferedImage qrCodeImage;
  private Graphics2D qrCodeGraphics;
  private int qrCodeDisplaySize;

  public QRProtoPanel(int qrCodeDisplaySize) {
    super();

    this.qrCodeImage = new BufferedImage(qrCodeDisplaySize, qrCodeDisplaySize, BufferedImage.TYPE_INT_RGB);
    qrCodeImage.createGraphics();
    this.qrCodeGraphics = (Graphics2D) qrCodeImage.getGraphics();
    this.qrCodeDisplaySize = qrCodeDisplaySize;

    setPreferredSize(new Dimension(qrCodeDisplaySize, qrCodeDisplaySize));
    displayNothing();
  }

  public void displayQRCode(QRCode qrCode) {
    BitMatrix bitMatrix = qrCode.generateBitMatrix();
    Dimension bitSize = new Dimension(qrCodeDisplaySize/bitMatrix.getWidth(), qrCodeDisplaySize/bitMatrix.getHeight());

    qrCodeGraphics.setColor(Color.WHITE);
    qrCodeGraphics.fillRect(0, 0, qrCodeDisplaySize, qrCodeDisplaySize);

    qrCodeGraphics.setColor(Color.BLACK);
    for(int y = 0; y < bitMatrix.getHeight(); y++)
      for(int x = 0; x < bitMatrix.getWidth(); x++)
        if(bitMatrix.get(x, y))
          qrCodeGraphics.fillRect(bitSize.width*x, bitSize.height*y, bitSize.width, bitSize.height);

    paintComponent(getGraphics()); // TODO: better solution? (repaint(0) is even slower)
  }

  public void displayNothing() {
    qrCodeGraphics.setColor(Color.WHITE);
    qrCodeGraphics.fillRect(0, 0, qrCodeDisplaySize, qrCodeDisplaySize);

    repaint(0);
  }

  public int getQRCodeSize() {
    return qrCodeDisplaySize;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    if(qrCodeImage != null)
      g.drawImage(qrCodeImage, 0, 0, this);
  }
}
