package qr_proto.util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;

public class ProfilerPanel extends JPanel {
  private static final int LEFT_AXIS_WIDTH = 200, BOTTOM_AXIS_WIDTH = 200, TOP_GAP = 40;
  private static final float DOT_SIZE = 5, DOT_SIZE_HALF = DOT_SIZE/2, BOTTOM_AXIS_LINE_LENGTH = 16;
  private static final Font
      graphFontSmall = new Font("San Francisco", Font.PLAIN, 20),
      graphFont = new Font("San Francisco", Font.PLAIN, 24);

  private BufferedImage graphImage;
  private Graphics2D graphGraphics;
  private Dimension graphSize;
  private HashMap<String, ProfilerData> data = new HashMap<>();
  private int dataSize = 0;
  private boolean removeZero = false;

  @SafeVarargs
  public ProfilerPanel(AbstractMap.SimpleEntry<String, Color>... types) {
    for(AbstractMap.SimpleEntry<String, Color> type: types)
      data.put(type.getKey(), new ProfilerData(type.getKey(), type.getValue()));
  }

  public void init() {
    graphSize = new Dimension(getWidth()*2, getHeight()*2);
    graphImage = new BufferedImage(graphSize.width, graphSize.height, BufferedImage.TYPE_INT_RGB);
    graphImage.createGraphics();
    graphGraphics = (Graphics2D) graphImage.getGraphics();
    graphGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    refreshGraph();
  }

  @SafeVarargs
  public final void addToData(AbstractMap.SimpleEntry<String, Float>... newData) {
    for(ProfilerData profilerData: data.values()) {
      float newDataPoint = 0;
      for(AbstractMap.SimpleEntry<String, Float> d: newData) {
        if(d.getKey().equals(profilerData.description)) {
          newDataPoint = d.getValue();
          break;
        }
      }
      profilerData.data.add(newDataPoint);
      profilerData.checkLength();
    }

    dataSize = data.get(data.keySet().iterator().next()).data.size();
    refreshGraph();
  }

  public void removeData(String description) {
    data.remove(description);

    refreshGraph();
  }

  private void refreshGraph() {
    checkDataSizes();

    if(dataSize > 0) {
      Dimension graphSizeWithoutAxes = new Dimension(graphSize.width - LEFT_AXIS_WIDTH, graphSize.height - BOTTOM_AXIS_WIDTH);
      float distance = (float) graphSizeWithoutAxes.width/Math.max(dataSize - 1, 1);

      // clear
      graphGraphics.setColor(Color.WHITE);
      graphGraphics.fill(new Rectangle2D.Float(0, 0, graphSize.width, graphSize.height));

      // axis lines
      graphGraphics.draw(new Line2D.Float(LEFT_AXIS_WIDTH, TOP_GAP, LEFT_AXIS_WIDTH, graphSizeWithoutAxes.height));
      graphGraphics.draw(new Line2D.Float(LEFT_AXIS_WIDTH, graphSizeWithoutAxes.height, graphSize.width, graphSizeWithoutAxes.height));

      // bottom axis
      for(int i = 0; i < dataSize; i++) {
        float x = LEFT_AXIS_WIDTH + i*distance;
        graphGraphics.draw(new Line2D.Float(x, graphSizeWithoutAxes.height, x, graphSizeWithoutAxes.height + BOTTOM_AXIS_LINE_LENGTH));
      }

      for(ProfilerData d: data.values()) {
        if(removeZero)
          d.data.removeFirst();
        if(dataSize == 1)
          d.data.addFirst(0f);

        float maxData = 0;
        for(float f: d.data)
          if(f > maxData)
            maxData = f;
        float heightScale = (graphSizeWithoutAxes.height - TOP_GAP)/maxData;

        graphGraphics.setColor(d.color);
        // lines
        for(int i = 1; i < data.size(); i++)
          graphGraphics.draw(new Line2D.Float(LEFT_AXIS_WIDTH + (i - 1)*distance, graphSizeWithoutAxes.height - d.data.get(i - 1)*heightScale, LEFT_AXIS_WIDTH + i*distance, graphSizeWithoutAxes.height - d.data.get(i)*heightScale));
        // dots and bottom axis
        for(int i = 0; i < data.size(); i++)
          graphGraphics.fill(new Ellipse2D.Float(LEFT_AXIS_WIDTH + i*distance - DOT_SIZE_HALF, graphSizeWithoutAxes.height - d.data.get(i)*heightScale - DOT_SIZE_HALF, DOT_SIZE, DOT_SIZE));
      }
    } else {
      String msg = "no data.";
      graphGraphics.setFont(graphFont);
      graphGraphics.drawString(msg, (graphSize.width - graphGraphics.getFontMetrics().stringWidth(msg))/2, (graphSize.height - graphGraphics.getFontMetrics().getHeight())/2);
    }

    if(removeZero) {
      dataSize--;
      removeZero = false;
    }
    if(dataSize == 1) {
      dataSize++;
      removeZero = true;
    }

    paintComponent(getGraphics());
  }

  private void checkDataSizes() {
    for(ProfilerData profilerData: data.values())
      if(profilerData.data.size() != dataSize)
        throw new IllegalStateException("ProfilerData sizes do not match.");
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

  class ProfilerData {
    private static final int MAX_DATA_SIZE = 10;

    public String description;
    public Color color;
    public LinkedList<Float> data;

    public ProfilerData(String description, Color color) {
      this.description = description;
      this.color = color;
      this.data = new LinkedList<>();
    }

    public ProfilerData(String description, Color color, LinkedList<Float> data) {
      this.description = description;
      this.color = color;
      this.data = data;
    }

    void checkLength() {
      while(data.size() > MAX_DATA_SIZE)
        data.removeFirst();
    }
  }
}
