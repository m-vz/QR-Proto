package audio;


import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.InputMismatchException;
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

  private AudioFormat format;
  private TargetDataLine line;
  private byte buffer[];
  private QRProto qrProto;
  private LinkedList<String> recordedMessages;
  private DecimalFormat formatter;

  public QRPhone(QRProto proto) {
    this(4000 /*4000 minimum*/, 8, 1, true, true, proto);
  }

  private QRPhone(float sampleRate, int sampleSizeInBits, int channels, boolean signed,
      boolean bigEndian, QRProto qrProto) {
    format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    try {
      Info info = new Info(TargetDataLine.class, format);
      line = (TargetDataLine) AudioSystem.getLine(info); //~= input stream
      line.open(format);
      line.start();
      int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
      buffer = new byte[bufferSize];
    } catch (LineUnavailableException e) {//TODO: handle exception
    }

    this.qrProto = qrProto;
    this.recordedMessages = new LinkedList<>();
    this.formatter = new DecimalFormat("000000");
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
    int inputlength = bytes.length;
    StringBuilder output = new StringBuilder();

    if (inputlength < 1000000) {
      output.append(formatter.format(inputlength));
    } else {
      throw new InputMismatchException(
          "Audio-Input longer than 999999 symbols. Might cause compression errors");//TODO: use Base64 for compression
    }

    for (int i = 0; i < inputlength; i++) {
      char byteToHandle = (char) (bytes[i] + 128);
      if (byteToHandle == '\u0080') {
        long zeroesToAdd = 1;
        while (i + 1 < inputlength && (char) (bytes[i + 1] + 128) == '\u0080') {
          zeroesToAdd++;
          i++;
        }
        if (zeroesToAdd <= 6) {
          for (int j = 0; j < zeroesToAdd; j++) {
            output.append('\u0080');
          }
        } else {
          output.append('\\');
          output.append(formatter.format(zeroesToAdd));
        }
      } else if (byteToHandle == '\\') {
        output.append("\\\\");
      } else {
        output.append(byteToHandle);
      }
    }
    Log.outln("Compressed message " + (float) inputlength / output.length() + " fold.");
    return output.toString();
  }

  public ByteArrayOutputStream convertStringToByteArrayOutputStream(String input) {

    int outputLength = Integer.parseInt(input.substring(0, 6));
    byte[] output = new byte[outputLength];
    int j = 0;
    for (int i = 6; i < input.length(); i++) {
      char insert = input.charAt(i);
      if (insert == '\\') {
        if (input.charAt(i + 1) == '\\') {
          output[j] = (byte) (insert);
          output[j] -= 128;
          j++;
          i++;
        } else {
          for (int k = 0; k < Integer.parseInt(input.substring(i + 1, i + 7)); k++) {
            output[j] = 0;
            j++;
          }
          i += 6;
        }
      } else {
        output[j] = (byte) (insert);
        output[j] -= 128;
        j++;
      }
    }
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(output.length);
    outputStream.write(output, 0, output.length);

    return outputStream;
  }


  class RecordMessages implements Runnable {

    @Override
    public void run() {
      Log.outln("Thread started");
      long RECORDING_TIME = 2000;
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
    new Thread(new RecordMessages()).start();
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
