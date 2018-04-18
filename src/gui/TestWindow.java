package gui;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import qr_proto.gui.QRProtoPanel;

import javax.swing.*;
import java.awt.*;

public class TestWindow extends JFrame {
  private WebcamPanel webcamPanel;
  private QRProtoPanel qrProtoPanel;

  public TestWindow(QRProtoPanel qrProtoPanel) throws HeadlessException {
    super();

    setLayout(new FlowLayout());
    setTitle("QR-Proto");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    Dimension size = WebcamResolution.VGA.getSize();
    Webcam webcam = Webcam.getWebcams().get(0);
    webcam.setViewSize(size);

    webcamPanel = new WebcamPanel(webcam);
    webcamPanel.setPreferredSize(size);
    webcamPanel.setFPSDisplayed(true);

    this.qrProtoPanel = qrProtoPanel;

    add(webcamPanel);
    add(qrProtoPanel);

    pack();
    setVisible(true);
  }

  public WebcamPanel getWebcamPanel() {
    return webcamPanel;
  }

  public QRProtoPanel getQRProtoPanel() {
    return qrProtoPanel;
  }
}
