package gui;

import audio.QRPhone;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import qr_proto.Log;
import qr_proto.QRProto;
import qr_proto.gui.QRProtoPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class TestWindow extends JFrame {
  public static final Color background = Color.WHITE;

  private WebcamImagePanel webcamPanel;
  private TestPanel testPanel;
  private QRProtoPanel qrProtoPanel;
  private QRProto qrProto;
  private QRPhone qrPhone;

  public TestWindow(QRProto qrProto) throws HeadlessException {
    super();

    this.qrProto = qrProto;
    this.qrPhone = new QRPhone();

    setLayout(new GridBagLayout());
    GridBagConstraints c;
    setTitle("QR-Proto");
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    addWindowListener(new WindowListener() {
      @Override
      public void windowOpened(WindowEvent e) {

      }

      @Override
      public void windowClosing(WindowEvent e) {
        Log.outln("Test window closing...");
        qrProto.end();
      }

      @Override
      public void windowClosed(WindowEvent e) {
        Log.outln("Test window closed.");
      }

      @Override
      public void windowIconified(WindowEvent e) {

      }

      @Override
      public void windowDeiconified(WindowEvent e) {

      }

      @Override
      public void windowActivated(WindowEvent e) {

      }

      @Override
      public void windowDeactivated(WindowEvent e) {

      }
    });
    System.setProperty("apple.eawt.quitStrategy", "SYSTEM_EXIT_0");

    webcamPanel = new WebcamImagePanel(Webcam.getWebcams().get(0));
    webcamPanel.setDrawMode(WebcamPanel.DrawMode.FIT);
    webcamPanel.setFPSDisplayed(true);
    c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.gridx = 0;
    c.gridy = 0;
    add(webcamPanel, c);

    this.testPanel = new TestPanel();
    qrProto.setConnectedCallback(new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        testPanel.disconnectButton.setEnabled(true);
        testPanel.testButton.setEnabled(true);
      }
    });
    qrProto.setConnectingCallback(new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        testPanel.connectButton.setEnabled(false);
      }
    });
    qrProto.setDisconnectedCallback(new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        testPanel.connectButton.setEnabled(true);
      }
    });
    qrProto.setCanSendCallback(new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

      }
    });
    qrProto.setReceivedCallback(new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        qrPhone.playAudio(qrPhone.convertStringToByteArrayOutputStream(qrProto.getReceivedMessage()));
      }
    });
    qrProto.setErrorCallback(new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

      }
    });
    c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.gridx = 0;
    c.gridy = 1;
    add(testPanel, c);

    this.qrProtoPanel = qrProto.getPanel();
    c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.gridx = 1;
    c.gridy = 0;
    c.gridheight = 2;
    add(qrProtoPanel, c);

    setBackground(background);
    getContentPane().setBackground(background);

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
    JButton connectButton, disconnectButton, testButton, resetButton, clearButton;

    TestPanel() {
      super();

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
          qrProto.sendMessage(qrPhone.convertAudioToString(qrPhone.recordAudio(3000)));
        }
      });
      testButton.setEnabled(false);
      resetButton = new JButton(new AbstractAction("reset") { // TODO: atm, the webcam panel breaks after reset.
        @Override
        public void actionPerformed(ActionEvent e) {
          resetButton.setEnabled(false);
          qrProto.reset();
          connectButton.setEnabled(true);
          disconnectButton.setEnabled(false);
          testButton.setEnabled(false);
          resetButton.setEnabled(true);
        }
      });
      clearButton = new JButton(new AbstractAction("clear") {
        @Override
        public void actionPerformed(ActionEvent e) {
          qrProto.clear();
        }
      });

      setBackground(background);

      add(connectButton);
      add(disconnectButton);
      add(testButton);
      add(resetButton);
      add(clearButton);
    }
  }

  class WebcamImagePanel extends WebcamPanel {
    Dimension size = WebcamResolution.QVGA.getSize();

    public WebcamImagePanel(Webcam webcam) {
      super(webcam);

      init();
    }

    public WebcamImagePanel(Webcam webcam, boolean start) {
      super(webcam, start);

      init();
    }

    public WebcamImagePanel(Webcam webcam, Dimension size, boolean start) {
      super(webcam, size, start);

      init();
    }

    public WebcamImagePanel(Webcam webcam, Dimension size, boolean start, ImageSupplier supplier) {
      super(webcam, size, start, supplier);

      init();
    }

    private void init() {
      setBackground(background);
    }

    @Override
    public Dimension getPreferredSize() {
      return size;
    }

    @Override
    public Dimension getMinimumSize() {
      return size;
    }
  }
}
