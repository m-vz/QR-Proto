import gui.TestWindow;
import qr_proto.QRProto;

public class Test {
    private QRProto qrProto;
    private TestWindow window;

    private void startTest() {
      window = new TestWindow();
      qrProto = new QRProto(window.getQRProtoPanel());
    }

    public static void main(String[] args) {
        Test test = new Test();
        test.startTest();
    }
}
