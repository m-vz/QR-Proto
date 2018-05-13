package audio;

import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.AbstractAction;
import qr_proto.util.Log;
import qr_proto.QRProto;

public class QRPhone {

  private AudioFormat format;
  private TargetDataLine recordingLine;
  private SourceDataLine playbackLine;
  private byte recodingBuffer[], playbackBuffer[];
  private QRProto qrProto;
  private LinkedList<String> recordedMessages, playbackMessages;
  private Recorder recorder;
  private Player player;
  private QRPhonePanel panel;
  private boolean canSend = false, sending = false, receiving = false;
  private DecimalFormat formatter;

  public QRPhone(QRProto proto) {
    this(4000 /*4000 minimum*/, 8, 1, true, true, proto);
  }

  private QRPhone(float sampleRate, int sampleSizeInBits, int channels, boolean signed,
      boolean bigEndian, QRProto qrProto) {
    format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    try {
      DataLine.Info info = new Info(TargetDataLine.class, format);
      recordingLine = (TargetDataLine) AudioSystem.getLine(info); //~= input stream
      recordingLine.open(format);
      recordingLine.start();

      info = new DataLine.Info(SourceDataLine.class, format);
      playbackLine = (SourceDataLine) AudioSystem.getLine(info);
      playbackLine.open(format);
      playbackLine.start();

      int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
      recodingBuffer = new byte[bufferSize];
      playbackBuffer = new byte[bufferSize];
    } catch (LineUnavailableException e) { // TODO: handle exception
    }

    this.qrProto = qrProto;
    this.formatter = new DecimalFormat("000000");
    this.recordedMessages = new LinkedList<>();
    this.playbackMessages = new LinkedList<>();
    this.panel = new QRPhonePanel(this);
  }

  public ByteArrayOutputStream recordAudio(long recordingTime) {
    long startTime = System.currentTimeMillis();
    boolean timeout = false;

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    try {
      while (!timeout) {
        int count = recordingLine.read(recodingBuffer, 0, recodingBuffer.length);
        if (count > 0)
          out.write(recodingBuffer, 0, count);
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

      int count;
      while ((count = ais.read(playbackBuffer, 0, playbackBuffer.length)) != -1) {
        if (count > 0)
          playbackLine.write(playbackBuffer, 0, count);
      }
      playbackLine.drain();

    } catch (IOException e) {//TODO: handle exception
    }
  }

  public ByteArrayOutputStream inflate (String inputString){

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    int outputLength = Integer.parseInt(inputString.substring(0,6));
    Inflater decompressor = new Inflater();
    char[] charInput = inputString.toCharArray();
    byte[] byteInput = new byte[charInput.length-6];

    for (int i=0; i < byteInput.length; i++){
      byteInput[i] = (byte) (charInput[i+6] + 128);
    }
    decompressor.setInput(byteInput, 0, outputLength);
    byte[] result = new byte[outputLength];

    try {
      int resultLength = decompressor.inflate(result);
    } catch (DataFormatException e) {
      e.printStackTrace();
    }

    decompressor.end();
    outputStream.write(result,0,outputLength);

    return outputStream;

  }


  public String deflate (ByteArrayOutputStream inputStream) {

    byte[] input = inputStream.toByteArray();
    byte[] output = new byte[input.length];
    Deflater compressor = new Deflater();
    compressor.setInput(input);
    compressor.finish();
    int compressedInputLength = compressor.deflate(output);
    compressor.end();

    char[] outputString = new char[compressedInputLength];
    for (int i=0; i < compressedInputLength; i++){
      outputString[i] = (char) (output[i] + 128);
    }
    String result = formatter.format(compressedInputLength);
    result += new String(outputString);
    Log.outln("Deflate compressed message " + (float) input.length / result.length() + " fold.");
    return result;
  }


  class Recorder implements Runnable {
    static final long RECORDING_TIME = 2000;

    boolean shouldStop = false;

    @Override
    public void run() {
      Log.outln("Thread started");
      qrProto.sendMessage(deflate(recordAudio(RECORDING_TIME)));
      while(!shouldStop) {
        synchronized(this) {
          if(!shouldStop)
            recordedMessages.add(deflate(recordAudio(RECORDING_TIME)));

          if(canSend && !recordedMessages.isEmpty())
            qrProto.sendMessage(recordedMessages.poll());
        }
      }
    }
  }

  class Player implements Runnable {
    boolean shouldStop = false;

    @Override
    public void run() {
      Log.outln("Thread started");
      while(!shouldStop) {
        synchronized(this) {
          if(!playbackMessages.isEmpty())
            playAudio(inflate(playbackMessages.poll()));
          else {
            try {
              Thread.sleep(10);
            } catch(InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }
  }

  public void stopRecording() {
    synchronized(this) {
      if(recorder != null)
        recorder.shouldStop = true;
      recordedMessages.clear();
    }
    sending = false;
  }

  public void stopPlayback() {
    synchronized(this) {
      if(player != null)
        player.shouldStop = true;
      playbackMessages.clear();
    }
    receiving = false;
  }

  public void startSending() {
    AbstractAction a = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        synchronized(this) {
          canSend = true;
        }
      }
    };
    qrProto.setCanSendCallback(a);

    recorder = new Recorder();
    new Thread(recorder).start();
    sending = true;
  }

  public void startReceiving() {
    AbstractAction a = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        playbackMessages.add(qrProto.getReceivedMessage());
      }
    };
    qrProto.setReceivedCallback(a);

    player = new Player();
    new Thread(player).start();
    receiving = true;
  }

  public QRPhonePanel getPanel() {
    return panel;
  }

  public boolean isSending() {
    return sending;
  }

  public boolean isReceiving() {
    return receiving;
  }
}
