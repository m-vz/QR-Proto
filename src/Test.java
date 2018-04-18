import gui.TestWindow;
import qr_proto.QRProto;

public class Test {
    private QRProto qrProto;
    private TestWindow window;

    private void startTest() {
      qrProto = new QRProto();
      window = new TestWindow(qrProto.getQRProtoPanel());

      qrProto.getSocket().connect();
    }

    public static void main(String[] args) {
        Test test = new Test();
        test.startTest();
    }
}
