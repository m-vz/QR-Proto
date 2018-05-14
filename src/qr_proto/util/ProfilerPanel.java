package qr_proto.util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;

public class ProfilerPanel extends JPanel {
  private static final int LEFT_AXIS_WIDTH = 240, BOTTOM_AXIS_WIDTH = 20, TOP_GAP = 40;
  private static final float DOT_SIZE = 5, DOT_SIZE_HALF = DOT_SIZE/2, LINE_LENGTH = 16, LEFT_TEXT_PADDING = 14;
  private static final Font
      graphFontSmall = new Font("San Francisco", Font.PLAIN, 20),
      graphFont = new Font("San Francisco", Font.PLAIN, 24);

  private BufferedImage graphImage;
  private Graphics2D graphGraphics;
  private Dimension graphSize;
  private HashMap<String, ProfilerData> data = new HashMap<>();
  private int dataSize = 0;
  private boolean removeZero = false;

  public ProfilerPanel(ProfilerData... types) {
    for(ProfilerData type: types)
      data.put(type.description, new ProfilerData(type.description, type.unit, type.color));
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
      if(removeZero)
        dataSize--;

      Dimension graphSizeWithoutAxes = new Dimension(graphSize.width - LEFT_AXIS_WIDTH, graphSize.height - BOTTOM_AXIS_WIDTH);
      float distance = (float) graphSizeWithoutAxes.width/Math.max(dataSize - 1, 1);

      // clear
      graphGraphics.setColor(getBackground());
      graphGraphics.fill(new Rectangle2D.Float(0, 0, graphSize.width, graphSize.height));

      // axis lines
      graphGraphics.setColor(Color.BLACK);
      graphGraphics.setStroke(new BasicStroke(2));
      graphGraphics.draw(new Line2D.Float(LEFT_AXIS_WIDTH - LINE_LENGTH, TOP_GAP, LEFT_AXIS_WIDTH, TOP_GAP));
      graphGraphics.draw(new Line2D.Float(LEFT_AXIS_WIDTH, TOP_GAP, LEFT_AXIS_WIDTH, graphSizeWithoutAxes.height));
      graphGraphics.draw(new Line2D.Float(LEFT_AXIS_WIDTH, graphSizeWithoutAxes.height, graphSize.width, graphSizeWithoutAxes.height));

      // bottom axis
      for(int i = 0; i < dataSize; i++) {
        float x = LEFT_AXIS_WIDTH + i*distance;
        graphGraphics.draw(new Line2D.Float(x, graphSizeWithoutAxes.height, x, graphSizeWithoutAxes.height + LINE_LENGTH));
      }

      // descriptions
      int axisDescriptionCount = data.size();
      String text = "max:";
      float width = graphGraphics.getFontMetrics().stringWidth(text);
      float height = graphGraphics.getFontMetrics().getHeight();
      graphGraphics.drawString(text, LEFT_AXIS_WIDTH - LEFT_TEXT_PADDING - LINE_LENGTH - width, TOP_GAP + axisDescriptionCount*height + height/2);
      axisDescriptionCount += data.size() + 1;
      text = "average:";
      width = graphGraphics.getFontMetrics().stringWidth(text);
      height = graphGraphics.getFontMetrics().getHeight();
      graphGraphics.drawString(text, LEFT_AXIS_WIDTH - LEFT_TEXT_PADDING - LINE_LENGTH - width, TOP_GAP + axisDescriptionCount*height + height/2);
      axisDescriptionCount += data.size() + 1;
      text = "min:";
      width = graphGraphics.getFontMetrics().stringWidth(text);
      height = graphGraphics.getFontMetrics().getHeight();
      graphGraphics.drawString(text, LEFT_AXIS_WIDTH - LEFT_TEXT_PADDING - LINE_LENGTH - width, TOP_GAP + axisDescriptionCount*height + height/2);
      axisDescriptionCount = 0;

      for(ProfilerData d: data.values()) {
        if(removeZero)
          d.data.removeFirst();
        if(dataSize == 1)
          d.data.addFirst(0f);

        float maxData = 0, minData = Float.MAX_VALUE, average = 0;
        for(float f: d.data) {
          if(f > maxData)
            maxData = f;
          if(f < minData)
            minData = f;
          average += f;
        }
        average /= d.data.size();
        float heightScale = (graphSizeWithoutAxes.height - TOP_GAP)/maxData;

        graphGraphics.setColor(d.color);
        // axis descriptions
        text = d.description;
        width = graphGraphics.getFontMetrics().stringWidth(text);
        height = graphGraphics.getFontMetrics().getHeight();
        graphGraphics.drawString(text, LEFT_AXIS_WIDTH - LEFT_TEXT_PADDING - LINE_LENGTH - width, TOP_GAP + axisDescriptionCount*height + height/2);
        text = new DecimalFormat("######.00" + d.unit).format(minData);
        width = graphGraphics.getFontMetrics().stringWidth(text);
        height = graphGraphics.getFontMetrics().getHeight();
        graphGraphics.drawString(text, LEFT_AXIS_WIDTH - LEFT_TEXT_PADDING - LINE_LENGTH - width, TOP_GAP + (1 + data.size() + axisDescriptionCount)*height + height/2);
        text = new DecimalFormat("######.00" + d.unit).format(maxData);
        width = graphGraphics.getFontMetrics().stringWidth(text);
        height = graphGraphics.getFontMetrics().getHeight();
        graphGraphics.drawString(text, LEFT_AXIS_WIDTH - LEFT_TEXT_PADDING - LINE_LENGTH - width, TOP_GAP + (2*(1 + data.size()) + axisDescriptionCount)*height + height/2);
        text = new DecimalFormat("######.00" + d.unit).format(average);
        width = graphGraphics.getFontMetrics().stringWidth(text);
        height = graphGraphics.getFontMetrics().getHeight();
        graphGraphics.drawString(text, LEFT_AXIS_WIDTH - LEFT_TEXT_PADDING - LINE_LENGTH - width, TOP_GAP + (3*(1 + data.size()) + axisDescriptionCount)*height + height/2);
        axisDescriptionCount++;
        // lines
        for(int i = 1; i < Math.max(dataSize, 2); i++)
          graphGraphics.draw(new Line2D.Float(LEFT_AXIS_WIDTH + (i - 1)*distance, graphSizeWithoutAxes.height - d.data.get(i - 1)*heightScale, LEFT_AXIS_WIDTH + i*distance, graphSizeWithoutAxes.height - d.data.get(i)*heightScale));
        // dots
        for(int i = 0; i < Math.max(dataSize, 2); i++)
          graphGraphics.fill(new Ellipse2D.Float(LEFT_AXIS_WIDTH + i*distance - DOT_SIZE_HALF, graphSizeWithoutAxes.height - d.data.get(i)*heightScale - DOT_SIZE_HALF, DOT_SIZE, DOT_SIZE));
      }

      if(removeZero)
        removeZero = false;
      if(dataSize == 1) {
        dataSize++;
        removeZero = true;
      }
    } else {
      String msg = "no data.";
      graphGraphics.setFont(graphFont);
      graphGraphics.drawString(msg, (graphSize.width - graphGraphics.getFontMetrics().stringWidth(msg))/2, (graphSize.height - graphGraphics.getFontMetrics().getHeight())/2);
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

  public static class ProfilerData {
    private static final int MAX_DATA_SIZE = 20;

    public String description;
    public String unit;
    public Color color;
    public LinkedList<Float> data;

    public ProfilerData(String description, String unit, Color color) {
      this.description = description;
      this.unit = unit;
      this.color = color;
      this.data = new LinkedList<>();
    }

    public ProfilerData(String description, String unit, Color color, LinkedList<Float> data) {
      this.description = description;
      this.unit = unit;
      this.color = color;
      this.data = data;
    }

    void checkLength() {
      while(data.size() > MAX_DATA_SIZE)
        data.removeFirst();
    }
  }
}
