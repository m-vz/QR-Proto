package audio;

import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.LinkedList;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.AbstractAction;
import qr_proto.util.Log;
import qr_proto.QRProto;

public class QRPhone {
  private static final long RECORDING_TIME = 2000;
  private static final int MIXER_BUFFER_TIME_IN_SECONDS = 10;

  private AudioFormat format;
  private TargetDataLine recordingLine;
  private SourceDataLine playbackLine;
  private byte recodingBuffer[], playbackBuffer[]; // both can hold one second of data
  private QRProto qrProto;
  private LinkedList<String> recordedMessages, playbackMessages;
  private Recorder recorder;
  private Player player;
  private QRPhonePanel panel;
  private boolean canSend = true;
  private DecimalFormat formatter;

  public QRPhone(QRProto proto) {
    this(4000 /*4000 minimum*/, 8, 1, true, true, proto);
  }

  private QRPhone(float sampleRate, int sampleSizeInBits, int channels, boolean signed,
      boolean bigEndian, QRProto qrProto) {
    this.qrProto = qrProto;
    this.formatter = new DecimalFormat("000000");
    this.recordedMessages = new LinkedList<>();
    this.playbackMessages = new LinkedList<>();
    this.panel = new QRPhonePanel(this);

    format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    int bufferSize = (int) format.getFrameRate() * format.getFrameSize(); // with the constructor used above, frameRate == sampleRate
    recodingBuffer = new byte[bufferSize];
    playbackBuffer = new byte[bufferSize];
  }

  public void startRecorder() {
    try {
      recordingLine = (TargetDataLine) AudioSystem.getLine(new Info(TargetDataLine.class, format)); // ~= input stream
      recordingLine.open(format, recodingBuffer.length*MIXER_BUFFER_TIME_IN_SECONDS);
    } catch(LineUnavailableException e) {
      e.printStackTrace();
    }

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
  }

  @SuppressWarnings("Duplicates")
  public void stopRecorder(boolean force) {
    synchronized(this) {
      if(recorder != null)
        recorder.shouldStop = true;
      if(force)
        recordedMessages.clear();
    }
  }

  class Recorder implements Runnable {
    boolean shouldStop = false;

    @Override
    public void run() {
      Log.outln("Recorder started.");

      while(!shouldStop || !recordedMessages.isEmpty()) {
        synchronized(this) {
          if(!shouldStop)
            recordedMessages.add(ByteArrayOutputStreamtoString(recordAudio()));

          if(canSend && !recordedMessages.isEmpty()) {
            qrProto.sendMessage(recordedMessages.poll());
            canSend = false;
          }

          if(shouldStop && !canSend) { // sleep for a short time if no new data is being recorded but there is still data to be sent
            try {
              Thread.sleep(10);
            } catch(InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      }

      recordingLine.flush();
      recordingLine.close();

      Log.outln("Recorder stopped.");
      panel.canSendAgain();
    }

    private ByteArrayOutputStream recordAudio() {
      long startTime = System.currentTimeMillis();
      boolean timeout = false;

      ByteArrayOutputStream out = new ByteArrayOutputStream();

      recordingLine.start();

      while (!timeout) {
        int count = recordingLine.read(recodingBuffer, 0, recodingBuffer.length);
        if(count > 0)
          out.write(recodingBuffer, 0, count);

        timeout = System.currentTimeMillis() - startTime > RECORDING_TIME;
      }

      recordingLine.stop();

      return out; // a ByteArrayOutputStream doesn't need to be closed, so we return it without closing it first
    }

    private String ByteArrayOutputStreamtoString (ByteArrayOutputStream inputStream) {
      byte[] input = inputStream.toByteArray();
      char[] outputString = new char[input.length];

      for (int i=0; i < input.length; i++)
        outputString[i] = (char) (input[i] + 128);

      return new String(outputString);
    }
  }

  public void startPlayer() {
    try {
      playbackLine = (SourceDataLine) AudioSystem.getLine(new Info(SourceDataLine.class, format));
      playbackLine.open(format, playbackBuffer.length*MIXER_BUFFER_TIME_IN_SECONDS);
    } catch(LineUnavailableException e) {
      e.printStackTrace();
    }

    AbstractAction a = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        synchronized(this) {
          playbackMessages.add(qrProto.getReceivedMessage());
        }
      }
    };
    qrProto.setReceivedCallback(a);

    player = new Player();
    new Thread(player).start();
  }

  @SuppressWarnings("Duplicates")
  public void stopPlayer(boolean force) {
    synchronized(this) {
      if(player != null)
        player.shouldStop = true;
      if(force)
        playbackMessages.clear();
    }
  }

  class Player implements Runnable {
    boolean shouldStop = false;

    @Override
    public void run() {
      Log.outln("Player started.");

      while(!shouldStop) {
        synchronized(this) {
          if(!playbackMessages.isEmpty()) {
            playAudio(StringtoByteArrayInputStream(playbackMessages.pop()));
          } else {
            try {
              Thread.sleep(10);
            } catch(InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      }

      playbackLine.start();
      playbackLine.drain();
      playbackLine.stop();
      playbackLine.close();

      Log.outln("Player stopped.");
      panel.canReceiveAgain();
    }

    private void playAudio(ByteArrayInputStream input) {
      try {
        AudioInputStream inputStream = new AudioInputStream(input, format, input.available() / format.getFrameSize());

        playbackLine.start();

        int count;
        while ((count = inputStream.read(playbackBuffer, 0, playbackBuffer.length)) != -1)
          if (count > 0)
            playbackLine.write(playbackBuffer, 0, count);

        playbackLine.stop();

      } catch (IOException e) { // TODO: handle exception
      }
    }

    private ByteArrayInputStream StringtoByteArrayInputStream(String inputString){
      char[] charInput = inputString.toCharArray();
      byte[] byteInput = new byte[charInput.length];

      for (int i=0; i < byteInput.length; i++)
        byteInput[i] = (byte) (((byte) charInput[i]) - 128);

      return new ByteArrayInputStream(byteInput);
    }
  }

  public QRPhonePanel getPanel() {
    return panel;
  }
}
