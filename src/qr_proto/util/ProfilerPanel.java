package qr_proto.util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedList;

public class ProfilerPanel extends JPanel {
  private static final Font
      graphFontSmall = new Font("San Francisco", Font.PLAIN, 20),
      graphFont = new Font("San Francisco", Font.PLAIN, 24);

  private BufferedImage graphImage;
  private Graphics2D graphGraphics;
  private Dimension graphSize;
  private LinkedList<Float> data = new LinkedList<>();

  public void init() {
    graphSize = new Dimension(getWidth()*2, getHeight()*2);
    graphImage = new BufferedImage(graphSize.width, graphSize.height, BufferedImage.TYPE_INT_RGB);
    graphImage.createGraphics();
    graphGraphics = (Graphics2D) graphImage.getGraphics();
    graphGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    refreshGraph();
  }

  public void setData(LinkedList<Float> data) {
    this.data = data;
    refreshGraph();
  }

  public void addData(Float... data) {
    this.data.addAll(Arrays.asList(data));
    refreshGraph();
  }

  private void refreshGraph() {
    int leftAxisWidth = 200, bottomAxisWidth = 200, topGap = 40;
    Dimension graphSizeWithoutAxes = new Dimension(graphSize.width - leftAxisWidth, graphSize.height - bottomAxisWidth);
    if(data != null && data.size() > 0) {
      float distance = graphSizeWithoutAxes.width/Math.max(data.size() - 1, 1);
      float maxData = 0;
      for(float f: data)
        if(f > maxData)
          maxData = f;
      float heightScale = (graphSizeWithoutAxes.height - topGap)/maxData;
      if(data.size() == 1)
        data.addFirst(0f);
      // clear
      graphGraphics.setColor(Color.WHITE);
      graphGraphics.fill(new Rectangle2D.Float(0, 0, graphSize.width, graphSize.height));

      graphGraphics.setColor(Color.BLACK);
      // axis lines
      graphGraphics.draw(new Line2D.Float(leftAxisWidth, topGap, leftAxisWidth, graphSizeWithoutAxes.height));
      graphGraphics.draw(new Line2D.Float(leftAxisWidth, graphSizeWithoutAxes.height, graphSize.width, graphSizeWithoutAxes.height));
      // lines
      for(int i = 1; i < data.size(); i++) {
        graphGraphics.draw(new Line2D.Float(leftAxisWidth + (i - 1)*distance, graphSizeWithoutAxes.height - data.get(i - 1)*heightScale, leftAxisWidth + i*distance, graphSizeWithoutAxes.height - data.get(i)*heightScale));
      }
      // dots and bottom axis
      float dotSize = 5, dotSizeHalf = dotSize/2, lineLength = 16;
      for(int i = 0; i < data.size(); i++) {
        float x = leftAxisWidth + i*distance;
        graphGraphics.fill(new Ellipse2D.Float(x - dotSizeHalf, graphSizeWithoutAxes.height - data.get(i)*heightScale - dotSizeHalf, dotSize, dotSize));
        graphGraphics.draw(new Line2D.Float(x, graphSizeWithoutAxes.height, x, graphSizeWithoutAxes.height + lineLength));
      }
    } else {
      String msg = "no data.";
      graphGraphics.setFont(graphFont);
      graphGraphics.drawString("no data.", (graphSize.width - graphGraphics.getFontMetrics().stringWidth(msg))/2, (graphSize.height - graphGraphics.getFontMetrics().getHeight())/2);
    }

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
