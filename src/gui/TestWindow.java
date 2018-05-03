package gui;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import qr_proto.QRProto;
import qr_proto.gui.QRProtoPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class TestWindow extends JFrame {
  private WebcamPanel webcamPanel;
  private TestPanel testPanel;
  private QRProtoPanel qrProtoPanel;
  private QRProto qrProto;

  public TestWindow(QRProto qrProto) throws HeadlessException {
    super();

    this.qrProto = qrProto;

    setLayout(new FlowLayout());
    setTitle("QR-Proto");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    webcamPanel = new WebcamPanel(Webcam.getWebcams().get(0));
    webcamPanel.setPreferredSize(WebcamResolution.QVGA.getSize());
    webcamPanel.setFPSDisplayed(true);

    testPanel = new TestPanel();

    this.qrProtoPanel = qrProto.getPanel();

    add(webcamPanel);
    add(testPanel);
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

  class TestPanel extends JPanel {
    AbstractAction connectedCallback, disconnectedCallback;
    JButton connectButton, disconnectButton, testButton;

    TestPanel() {
      super();

      connectedCallback = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          disconnectButton.setEnabled(true);
          testButton.setEnabled(true);
        }
      };
      disconnectedCallback = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          connectButton.setEnabled(true);
        }
      };

      connectButton = new JButton(new AbstractAction("connect") {
        @Override
        public void actionPerformed(ActionEvent e) {
          qrProto.connect();
          connectButton.setEnabled(false);
        }
      });
      disconnectButton = new JButton(new AbstractAction("disconnect") {
        @Override
        public void actionPerformed(ActionEvent e) {
          qrProto.disconnect();
          disconnectButton.setEnabled(false);
          testButton.setEnabled(false);
          connectButton.setEnabled(true);
        }
      });
      disconnectButton.setEnabled(false);
      testButton = new JButton(new AbstractAction("test") {
        @Override
        public void actionPerformed(ActionEvent e) {
          qrProto.sendMessage("000001111122222333334444455555666667777788888999");
        }
      });
      testButton.setEnabled(false);

      qrProto.setConnectedCallback(connectedCallback);
      qrProto.setDisconnectedCallback(disconnectedCallback);

      add(connectButton);
      add(disconnectButton);
      add(testButton);
    }
  }
}
