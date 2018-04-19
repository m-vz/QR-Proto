package qr_proto;

import qr_proto.gui.QRProtoPanel;

import java.awt.*;

public class QRProto {
  private QRProtoSocket socket;

  public QRProto(int qrCodeSize, Dimension cameraResolution) {
    socket = new QRProtoSocket(qrCodeSize, cameraResolution);
  }

  public QRProtoPanel getQRProtoPanel() {
    return socket.getPanel();
  }

  public QRProtoSocket getSocket() {
    return socket;
  }
}
