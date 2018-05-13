package qr_proto.util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

public class ProfilerPanel extends JPanel {
  private BufferedImage graphImage;
  private Graphics2D graphGraphics;
  private Dimension graphSize;

  public void init() {
    graphSize = new Dimension(getWidth()*2, getHeight()*2);
    graphImage = new BufferedImage(graphSize.width, graphSize.height, BufferedImage.TYPE_INT_RGB);
    graphImage.createGraphics();
    graphGraphics = (Graphics2D) graphImage.getGraphics();
    graphGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    graphGraphics.setColor(Color.WHITE);
    graphGraphics.draw(new Line2D.Float(0, 0, graphSize.width, graphSize.height));
    graphGraphics.draw(new Line2D.Float(0, graphSize.height, graphSize.width, 0));
    refreshGraph();
  }

  public void refreshGraph() {
    paintComponent(getGraphics());
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    if(graphImage != null)
      g.drawImage(graphImage, 0, 0, getWidth(), getHeight(), 0, 0, graphSize.width, graphSize.height, this);
  }

  @Override
  public void setBackground(Color bg) {
    super.setBackground(bg);
  }
}
