package qr_proto;//package com.github.sarxos.example1;


import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.swing.JFrame;
import javax.swing.JTextArea;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import qr_proto.gui.ImagePanel;
import qr_proto.qr.QRGenerator;


public class QRProto extends JFrame implements Runnable, ThreadFactory {

  private static final long serialVersionUID = 6441489157408381878L;

  private Executor executor = Executors.newSingleThreadExecutor(this);

  private Webcam webcam = null;
  private WebcamPanel panel = null;
  private JTextArea textarea = null;
  private ImagePanel imagePanel = null;

  private QRGenerator generator = null;
  private String filepath =
      "/Users/Aeneas/Nextcloud/Studium/08. FS2018/Internet and Security/QR-Proto/img/";
  private String message = "some message\uD83D\uDE34";
  private String filetype = "png";
  private String filename = "2";

  public QRProto() {
    super();

    setLayout(new FlowLayout());
    setTitle("QR-Proto");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    Dimension size = WebcamResolution.QVGA.getSize();

    generator.makeQR(message, filename);

    webcam = Webcam.getWebcams().get(0);
    webcam.setViewSize(size);

    panel = new WebcamPanel(webcam);
    panel.setPreferredSize(size);
    panel.setFPSDisplayed(true);

    imagePanel = new ImagePanel(filepath + filename + "." + filetype);
    imagePanel.setPreferredSize(size);

    textarea = new JTextArea();
    textarea.setEditable(false);
    textarea.setPreferredSize(size);

    add(panel);
    add(imagePanel);
    add(textarea);

    pack();
    setVisible(true);

    executor.execute(this);
  }

  @Override
  public void run() {

    do {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      BufferedImage image = null;
      Result result = null;
      String decodedQR = null;

      if (webcam.isOpen()) {

        if ((image = webcam.getImage()) == null) {
          continue;
        }

        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        try {
          result = new MultiFormatReader().decode(bitmap);
        } catch (NotFoundException e) {
          // fall thru, it means there is no QR code in image
        }
      }

      if (result != null) {
        String checkedMessage =result.getText();// generator.checkMessage(result.getText());
        if (checkedMessage != null) {
          textarea.setText(checkedMessage);
        }
      }

    } while (true);
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread t = new Thread(r, "example-runner");
    t.setDaemon(true);
    return t;
  }
}
