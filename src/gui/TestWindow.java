package gui;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import qr_proto.gui.QRProtoPanel;

import javax.swing.*;
import java.awt.*;

public class TestWindow extends JFrame {
  private WebcamPanel webcamPanel;
  private JTextArea testPanel;
  private QRProtoPanel qrProtoPanel;

  public TestWindow() throws HeadlessException {
    super();


    setLayout(new FlowLayout());
    setTitle("QR-Proto");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    Dimension size = WebcamResolution.QVGA.getSize();
    Webcam webcam = Webcam.getWebcams().get(0);
    webcam.setViewSize(size);

    webcamPanel = new WebcamPanel(webcam);
    webcamPanel.setPreferredSize(size);
    webcamPanel.setFPSDisplayed(true);

    qrProtoPanel = new QRProtoPanel(size.height);

    testPanel = new JTextArea();
    testPanel.setEditable(false);
    testPanel.setPreferredSize(size);

    add(webcamPanel);
    add(qrProtoPanel);
    add(testPanel);

    pack();
    setVisible(true);
  }

  public WebcamPanel getWebcamPanel() {
    return webcamPanel;
  }

  public JTextArea getTestPanel() {
    return testPanel;
  }

  public QRProtoPanel getQRProtoPanel() {
    return qrProtoPanel;
  }
}
