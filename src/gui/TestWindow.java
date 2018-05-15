package gui;

import audio.QRPhone;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import qr_proto.util.Log;
import qr_proto.QRProto;
import qr_proto.gui.QRProtoPanel;
import qr_proto.util.Profiler;
import qr_proto.util.ProfilerPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.AbstractMap;
import java.util.Random;

import static qr_proto.util.Config.NUMBER_OF_TEST_MESSAGES;

public class TestWindow extends JFrame {
  public static final Color background = Color.WHITE;

  private WebcamImagePanel webcamPanel;
  private TestPanel testPanel;
  private QRProtoPanel qrProtoPanel;
  private ProfilerPanel profilerPanel;
  private QRProto qrProto;
  private QRPhone qrPhone;

  public TestWindow(QRProto qrProto) throws HeadlessException {
    super();

    this.qrProto = qrProto;
    this.qrPhone = new QRPhone(qrProto);

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
        qrPhone.stopRecording();
        qrPhone.stopPlayback();
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

    testPanel = new TestPanel();
    qrProto.setConnectedCallback(new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        testPanel.disconnectButton.setEnabled(true);
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
    c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.gridx = 0;
    c.gridy = 1;
    add(testPanel, c);

    c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.NORTH;
    c.gridx = 0;
    c.gridy = 2;
    add(qrPhone.getPanel(), c);

    profilerPanel = new ProfilerPanel(
        new ProfilerPanel.ProfilerData("round trip time", "ms", new Color(76, 54, 124)),
        new ProfilerPanel.ProfilerData("bits per second", "bps", new Color(173, 90, 54)),
        new ProfilerPanel.ProfilerData("errors per msg", "err", new Color(173, 20, 31))
    );
    c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.anchor = GridBagConstraints.NORTH;
    c.gridx = 0;
    c.gridy = 3;
    add(profilerPanel, c);

    qrProtoPanel = qrProto.getPanel();
    c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.gridx = 1;
    c.gridy = 0;
    c.gridheight = 4;
    add(qrProtoPanel, c);

    setBackground(background);
    getContentPane().setBackground(background);
    qrPhone.getPanel().setBackground(Color.ORANGE);
    profilerPanel.setBackground(background);

    pack();
    profilerPanel.init();

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
          connectButton.setEnabled(true);
        }
      });
      disconnectButton.setEnabled(false);
      testButton = new JButton(new AbstractAction("test") {
        @Override
        public void actionPerformed(ActionEvent e) {
          int size = 10000;
          new Thread(new Runnable() {
            private int numErrors = 0, timesSent = 0;

            @Override
            public void run() {
              qrProto.setErrorCallback(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  numErrors++;
                }
              });
              qrProto.setCanSendCallback(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  long roundTripTime = Profiler.endMeasurement("rtt");
                  float bytesPerSeconds = size/(Math.max((float) roundTripTime, 1)/1000);
                  Profiler.profileData("bps", bytesPerSeconds);
                  Profiler.profileData("err", numErrors);
                  profilerPanel.addToData(
                      new AbstractMap.SimpleEntry<>("round trip time", (float) roundTripTime),
                      new AbstractMap.SimpleEntry<>("bits per second", bytesPerSeconds),
                      new AbstractMap.SimpleEntry<>("errors per msg", (float) numErrors)
                  );
                  numErrors = 0;
                  timesSent++;

                  if(timesSent <= NUMBER_OF_TEST_MESSAGES) {
                    Profiler.startMeasurement("rtt");
                    StringBuilder testData = new StringBuilder();
                    for(char i = 0; i < size; i++)
                      testData.append(i);
                    qrProto.sendMessage(testData.toString());
                  }
                }
              });
              Profiler.startMeasurement("rtt");
              StringBuilder testData = new StringBuilder();
              for(char i = 0; i < size; i++)
                testData.append(i);
              qrProto.sendMessage(testData.toString());
            }
          }).start();
//          Random r1 = new Random(), r2 = new Random();
//          for(int i = 0; i < 100; i++) {
//            profilerPanel.addToData(
//                new AbstractMap.SimpleEntry<>("round trip time", r1.nextFloat()*100),
//                new AbstractMap.SimpleEntry<>("bits per second", r2.nextFloat()*100*8),
//                new AbstractMap.SimpleEntry<>("errors per qr code", r2.nextFloat()*5)
//            );
//            try {
//              Thread.sleep(100);
//            } catch(InterruptedException e1) {
//              e1.printStackTrace();
//            }
//          }
        }
      });
      resetButton = new JButton(new AbstractAction("reset") { // TODO: atm, the webcam breaks after reset.
        @Override
        public void actionPerformed(ActionEvent e) {
          resetButton.setEnabled(false);
          qrProto.reset();
          qrPhone.stopRecording();
          qrPhone.stopPlayback();
          connectButton.setEnabled(true);
          disconnectButton.setEnabled(false);
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
