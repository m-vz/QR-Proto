package audio;


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
        long charsToSkip = 1;
        while ((char) (bytes[i++] + 128) == '\u0080' && i < inputlength) {
          charsToSkip++;
        }
        if (charsToSkip <= 6) {
          for (int j = 0; j < charsToSkip; j++) {
            output.append('\u0080');
          }
        } else {
          output.append('\\');
          output.append(formatter.format(charsToSkip));
        }
      } else if (byteToHandle == '\\') {
        output.append("\\\\");
      } else {
        output.append(byteToHandle);
      }
    }
    Log.outln("Compressed message " + inputlength / output.length() + " fold.");
    if (inputlength / output.length() > 600) {
      Log.errln(output.toString());
    }
    return output.toString();
  }

  public ByteArrayOutputStream convertStringToByteArrayOutputStream(String input) {

    int outputLength = Integer.parseInt(input.substring(0, 6));
    byte[] output = new byte[outputLength];
    for (int i = 6; i < outputLength; i++) {
      char insert = input.charAt(i);
      if (insert == '\\') {
        i++;
        if (input.charAt(i) == '\\') {
          output[i] = (byte) (insert);
          output[i] -= 128;
        } else {
          for (int j = 0; j < Integer.parseInt(input.substring(i, i + 6)); j++) {
            output[i] = 0;
          }
          i += 6;
        }
      }
      output[i] = (byte) (insert);
      output[i] -= 128;
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
