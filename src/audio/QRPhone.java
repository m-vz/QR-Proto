package audio;


import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.AbstractAction;
import qr_proto.Log;
import qr_proto.QRProto;

public class QRPhone {

  private final long RECORDING_TIME = 500;
  private AudioFormat format;
  private DataLine.Info info;
  private TargetDataLine line;
  private byte buffer[];
  private QRProto qrProto;
  private LinkedList<String> recordedMessages;

  public QRPhone(QRProto proto) {
    this(4000 /*4000 minimum*/, 8, 1, true, true, proto);
  }

  public QRPhone(float sampleRate, int sampleSizeInBits, int channels, boolean signed,
      boolean bigEndian, QRProto qrProto) {
    format = new AudioFormat(sampleRate,
        sampleSizeInBits, channels, signed, bigEndian);
    try {
      info = new Info(TargetDataLine.class, format);
      line = (TargetDataLine) AudioSystem.getLine(info); //~= input stream
      line.open(format);
      line.start();
      int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
      buffer = new byte[bufferSize];
    } catch (LineUnavailableException e) {//TODO: handle exception
    }

    this.qrProto = qrProto;
    this.recordedMessages = new LinkedList<>();

    RecordMessages recordMessages = new RecordMessages();
    new Thread(recordMessages).start();
  }

  public ByteArrayOutputStream recordAudio(long recordingTime) {
    long startTime = System.currentTimeMillis();
    boolean timeout = false;

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    try {
      while (!timeout) {
        int count = line.read(buffer, 0, buffer.length);
        if (count > 0) {
          out.write(buffer, 0, count);
        }
        timeout = System.currentTimeMillis() - startTime > recordingTime;
      }
      out.close();
    } catch (IOException e) {//TODO: handle exception
    }
    return out;
  }

  public void playAudio(ByteArrayOutputStream out) {
    try {
      byte audio[] = out.toByteArray();
      InputStream input = new ByteArrayInputStream(audio);
      AudioInputStream ais = new AudioInputStream(input, format,
          audio.length / format.getFrameSize());

      DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
      SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
      line.open(format);
      line.start();

      int count;
      while ((count = ais.read(buffer, 0, buffer.length)) != -1) {
        if (count > 0) {
          line.write(buffer, 0, count);
        }
      }
      line.drain();
      line.close();

    } catch (IOException e) {//TODO: handle exception
    } catch (LineUnavailableException e) { //TODO: handle exception
    }
  }

  public String convertAudioToString(ByteArrayOutputStream stream) {
    byte[] bytes = stream.toByteArray();
    return new String(toCharArray(bytes));
  }

  public ByteArrayOutputStream convertStringToByteArrayOutputStream(String input) {
    byte[] output = toByteArray(input.toCharArray());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(output.length);
    outputStream.write(output, 0, output.length);

    return outputStream;
  }

  char[] toCharArray(byte[] bytes) {
    int length = bytes.length;
    char[] chars = new char[length];

    for (int i = 0; i < length; i++) {
      chars[i] = (char) (bytes[i] + 128);
    }

    return chars;
  }

  byte[] toByteArray(char[] chars) {
    int length = chars.length;
    byte[] bytes = new byte[length];
    for (int i = 0; i < length; i++) {
      bytes[i] = (byte) (chars[i]);
      bytes[i] -= 128;
    }
    return bytes;
  }


  class RecordMessages implements Runnable {

    LinkedList<String> recordedMessages;

    public RecordMessages() {
      recordedMessages = QRPhone.this.recordedMessages;
    }

    @Override
    public void run() {
      Log.outln("Thread started");
      qrProto.sendMessage(convertAudioToString(recordAudio(RECORDING_TIME)));
      while (true) {
        recordedMessages.add(convertAudioToString(recordAudio(RECORDING_TIME)));
      }
    }
  }

  public void startSending() {
    AbstractAction a = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        qrProto.sendMessage(recordedMessages.poll());
      }
    };
    qrProto.setCanSendCallback(a);
  }

  public void startReceiving() {
    AbstractAction a = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        playAudio(convertStringToByteArrayOutputStream(qrProto.getReceivedMessage()));
      }
    };
    qrProto.setReceivedCallback(a);
  }
}
