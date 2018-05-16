package qr_proto.gui;

import com.google.zxing.common.BitMatrix;
import java.awt.geom.Rectangle2D;
import qr_proto.qr.QRCode;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class QRProtoPanel extends JPanel {
  private BufferedImage qrCodeImage;
  private Graphics2D qrCodeGraphics;
  private int qrCodeDisplaySize;

  public QRProtoPanel(int qrCodeDisplaySize) {
    super();

    qrCodeImage = new BufferedImage(qrCodeDisplaySize, qrCodeDisplaySize, BufferedImage.TYPE_INT_RGB);
    qrCodeImage.createGraphics();
    qrCodeGraphics = (Graphics2D) qrCodeImage.getGraphics();
    qrCodeGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    this.qrCodeDisplaySize = qrCodeDisplaySize;

    displayNothing();
  }

  public void displayQRCode(QRCode qrCode) {
    BitMatrix bitMatrix = qrCode.generateBitMatrix();
    float bitWidth = (float) qrCodeDisplaySize/bitMatrix.getWidth(), bitHeight = (float) qrCodeDisplaySize/bitMatrix.getHeight();

    qrCodeGraphics.setColor(Color.WHITE);
    qrCodeGraphics.fillRect(0, 0, qrCodeDisplaySize, qrCodeDisplaySize);

    Color color;
    switch(qrCode.getType()) {
      case ACK:
        color = new Color(83, 198, 0);
        break;
      case ERR:
        color = new Color(198, 49, 0);
        break;
      default:
        color = Color.LIGHT_GRAY;
    }
    qrCodeGraphics.setColor(color);
    qrCodeGraphics.setStroke(new BasicStroke(10));
    qrCodeGraphics.draw(new Rectangle2D.Float(5, 5, qrCodeDisplaySize - 10, qrCodeDisplaySize - 10));

    qrCodeGraphics.setColor(Color.BLACK);
    for(int y = 0; y < bitMatrix.getHeight(); y++)
      for(int x = 0; x < bitMatrix.getWidth(); x++)
        if(bitMatrix.get(x, y))
          qrCodeGraphics.fill(new Rectangle2D.Float(bitWidth*x, bitHeight*y, bitWidth, bitHeight));

    paintComponent(getGraphics());
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

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(qrCodeDisplaySize, qrCodeDisplaySize);
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(qrCodeDisplaySize, qrCodeDisplaySize);
  }
}
