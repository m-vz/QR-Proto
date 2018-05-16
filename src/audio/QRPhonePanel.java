package audio;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class QRPhonePanel extends JPanel {
  private JButton startSendingButton, startReceivingButton, stopSendingButton, stopReceivingButton, resetButton;
  private JPanel startPanel, stopPanel, controlPanel;

  QRPhonePanel(QRPhone phone) {
    setLayout(new BorderLayout());

    startPanel = new JPanel(new FlowLayout());
    stopPanel = new JPanel(new FlowLayout());
    controlPanel = new JPanel(new BorderLayout());
    JLabel phoneHeading = new JLabel("QRPhone");
    startSendingButton = new JButton(new AbstractAction("start sending") {
      @Override
      public void actionPerformed(ActionEvent e) {
        phone.startRecorder();
        startSendingButton.setEnabled(false);
        stopSendingButton.setEnabled(true);
      }
    });
    startReceivingButton = new JButton(new AbstractAction("start receiving") {
      @Override
      public void actionPerformed(ActionEvent e) {
        phone.startPlayer();
        startReceivingButton.setEnabled(false);
        stopReceivingButton.setEnabled(true);
      }
    });
    stopSendingButton = new JButton(new AbstractAction("stop sending") {
      @Override
      public void actionPerformed(ActionEvent e) {
        phone.stopRecorder(false);
        stopSendingButton.setEnabled(false);
      }
    });
    stopSendingButton.setEnabled(false);
    stopReceivingButton = new JButton(new AbstractAction("stop receiving") {
      @Override
      public void actionPerformed(ActionEvent e) {
        phone.stopPlayer(false);
        stopReceivingButton.setEnabled(false);
      }
    });
    stopReceivingButton.setEnabled(false);
    resetButton = new JButton(new AbstractAction("reset phone") {
      @Override
      public void actionPerformed(ActionEvent e) {
        phone.stopRecorder(true);
        phone.stopPlayer(true);
        startSendingButton.setEnabled(true);
        startReceivingButton.setEnabled(true);
        stopSendingButton.setEnabled(false);
        stopReceivingButton.setEnabled(false);
      }
    });

    startPanel.add(startSendingButton);
    startPanel.add(startReceivingButton);
    stopPanel.add(stopSendingButton);
    stopPanel.add(stopReceivingButton);
    controlPanel.add(startPanel, BorderLayout.PAGE_START);
    controlPanel.add(stopPanel, BorderLayout.CENTER);
    controlPanel.add(resetButton, BorderLayout.PAGE_END);
    add(phoneHeading, BorderLayout.PAGE_START);
    add(controlPanel, BorderLayout.CENTER);
  }

  void canSendAgain() {
    startSendingButton.setEnabled(true);
  }

  void canReceiveAgain() {
    startReceivingButton.setEnabled(true);
  }

  @Override
  public void setBackground(Color bg) {
    super.setBackground(bg);
    if(startPanel != null)
      startPanel.setBackground(bg);
    if(stopPanel != null)
      stopPanel.setBackground(bg);
    if(controlPanel != null)
      controlPanel.setBackground(bg);
  }
}
