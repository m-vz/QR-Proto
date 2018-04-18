package qr_proto.gui;

import qr_proto.qr.QRCode;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class QRProtoPanel extends JPanel {
  private BufferedImage qrCodeImage = null;

  public QRProtoPanel(int size) {
    super();

    setPreferredSize(new Dimension(size, size));
  }

  public void displayQRCode(QRCode qrCode) {
    int width = qrCode.getBitMatrix().getWidth();
    //noinspection SuspiciousNameCombination
    qrCodeImage = new BufferedImage(width, width, BufferedImage.TYPE_INT_RGB);
    qrCodeImage.createGraphics();
    Graphics2D graphics = (Graphics2D) qrCodeImage.getGraphics();

    graphics.setColor(Color.WHITE);
    //noinspection SuspiciousNameCombination
    graphics.fillRect(0, 0, width, width);

    graphics.setColor(Color.BLACK);
    for(int y = 0; y < width; y++)
      for(int x = 0; x < width; x++)
        if(qrCode.getBitMatrix().get(x, y))
          graphics.fillRect(x, y, 1, 1);

    repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    if(qrCodeImage != null)
      g.drawImage(qrCodeImage, 0, 0, this);
  }
}
