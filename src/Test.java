import audio.QRPhone;
import com.github.sarxos.webcam.WebcamResolution;
import gui.TestWindow;
import java.io.ByteArrayOutputStream;
import qr_proto.QRProto;


public class Test {

  private QRProto qrProto;
  private TestWindow window;

  private void startTest() {
    qrProto = new QRProto(750, WebcamResolution.FHD.getSize());
    window = new TestWindow(qrProto);
  }

  public static void main(String[] args) {
    StringBuilder s = new StringBuilder();
    s.append("\\\\");
    s.append('a');
    Test test = new Test();
    test.startTest();
  }
}
