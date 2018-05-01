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

    this.qrProtoPanel = qrProto.getQRProtoPanel();

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
    AbstractAction connectedCallback;
    JButton connectButton, disconnectButton;

    TestPanel() {
      super();

      connectedCallback = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          disconnectButton.setEnabled(true);
          qrProto.getSocket().sendMessage("012345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s22345678 012345678 012345678 012345678 012345678 s32345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678 s12345678 012345678 012345678 012345678 012345678");
        }
      };
      connectButton = new JButton(new AbstractAction("connect") {
        @Override
        public void actionPerformed(ActionEvent e) {
          qrProto.getSocket().connect(connectedCallback);
          connectButton.setEnabled(false);
        }
      });
      disconnectButton = new JButton(new AbstractAction("disconnect") {
        @Override
        public void actionPerformed(ActionEvent e) {
          qrProto.getSocket().disconnect();
          disconnectButton.setEnabled(false);
          connectButton.setEnabled(true);
        }
      });
      disconnectButton.setEnabled(false);

      add(connectButton);
      add(disconnectButton);
    }
  }
}
