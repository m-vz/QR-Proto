package qr_proto.gui;

import com.google.zxing.common.BitMatrix;
import qr_proto.qr.QRCode;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class QRProtoPanel extends JPanel {
  private BufferedImage qrCodeImage;
  private Graphics2D qrCodeGraphics;
  private int size;

  public QRProtoPanel(int size) {
    super();

    this.size = size;
    this.qrCodeImage = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_BINARY);
    qrCodeImage.createGraphics();
    this.qrCodeGraphics = (Graphics2D) qrCodeImage.getGraphics();

    setPreferredSize(new Dimension(size, size));
    displayNothing();
  }

  public void displayQRCode(QRCode qrCode) {
    BitMatrix bitMatrix = qrCode.generateBitMatrix(size);

    qrCodeGraphics.setColor(Color.WHITE);
    qrCodeGraphics.fillRect(0, 0, size, size);

    qrCodeGraphics.setColor(Color.BLACK);
    for(int y = 0; y < size; y++)
      for(int x = 0; x < size; x++)
        if(bitMatrix.get(x, y))
          qrCodeGraphics.fillRect(x, y, 1, 1);

    repaint();
  }

  public void displayNothing() {
    qrCodeGraphics.setColor(Color.WHITE);
    qrCodeGraphics.fillRect(0, 0, size, size);
  }

  public int getQRCodeSize() {
    return size;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    if(qrCodeImage != null)
      g.drawImage(qrCodeImage, 0, 0, this);
  }
}
