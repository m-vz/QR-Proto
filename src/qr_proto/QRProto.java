package qr_proto;

import qr_proto.gui.QRProtoPanel;

import javax.swing.*;
import java.awt.*;

public class QRProto {
  private QRProtoSocket socket;

  public QRProto(int qrCodeSize, Dimension cameraResolution) {
    socket = new QRProtoSocket(qrCodeSize, cameraResolution);
  }

  public QRProtoPanel getPanel() {
    return socket.getPanel();
  }

  public void sendMessage(String message) {
    socket.sendMessage(message);
  }

  public void connect() {
    socket.connect();
  }

  public void disconnect() {
    socket.disconnect();
  }

  public void end() {
    socket.end();
  }

  public String getReceivedMessage() {
    return socket.getReceived();
  }

  public void setConnectedCallback(AbstractAction connectedCallback) {
    socket.setConnectedCallback(connectedCallback);
  }

  public void setConnectingCallback(AbstractAction connectingCallback) {
    socket.setConnectingCallback(connectingCallback);
  }

  public void setDisconnectedCallback(AbstractAction disconnectedCallback) {
    socket.setDisconnectedCallback(disconnectedCallback);
  }

  public void setCanSendCallback(AbstractAction canSendCallback) {
    socket.setCanSendCallback(canSendCallback);
  }

  public void setReceivedCallback(AbstractAction receivedCallback) {
    socket.setReceivedCallback(receivedCallback);
  }

  public void setErrorCallback(AbstractAction errorCallback) {
    socket.setErrorCallback(errorCallback);
  }
}
