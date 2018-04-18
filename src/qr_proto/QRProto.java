package qr_proto;

import qr_proto.gui.QRProtoPanel;

public class QRProto {
  private QRProtoSocket socket;

  public QRProto() {
    socket = new QRProtoSocket();
  }

  public QRProtoPanel getQRProtoPanel() {
    return socket.getPanel();
  }

  public QRProtoSocket getSocket() {
    return socket;
  }
}
