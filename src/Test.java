import com.github.sarxos.webcam.WebcamResolution;
import gui.TestWindow;
import qr_proto.QRProto;

import java.awt.*;

public class Test {
    private QRProto qrProto;
    private TestWindow window;

    private void startTest() {
      qrProto = new QRProto(600, WebcamResolution.VGA.getSize());
      window = new TestWindow(qrProto);
    }

    public static void main(String[] args) {
        Test test = new Test();
        test.startTest();
    }
}
