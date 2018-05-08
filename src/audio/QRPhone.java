package audio;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.IO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import qr_proto.Log;

public class QRPhone {
  private AudioFormat format;
  private DataLine.Info info;
  private TargetDataLine line;
  private byte buffer[];

  public QRPhone () {
    this(8000 /*4000 minimum*/,8,1,true,true);
  }
  public QRPhone (float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian) {
    format =  new AudioFormat(sampleRate,
        sampleSizeInBits, channels, signed, bigEndian);
    try {
      info = new Info(TargetDataLine.class, format);
      line = (TargetDataLine) AudioSystem.getLine(info); //~= input stream
      line.open(format);
      line.start();
      int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
      buffer = new byte[bufferSize];
    }
    catch (LineUnavailableException e){//TODO: handle exception
    }
  }

  public ByteArrayOutputStream recordAudio (long recordingTime) {
    long startTime = System.currentTimeMillis();
    boolean timeout = false;

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    try {
      while (!timeout) {
        int count = line.read(buffer, 0, buffer.length);
        if (count > 0) {
          out.write(buffer, 0, count);
        }
        Log.outln(String.valueOf(System.currentTimeMillis() - startTime));
        timeout = System.currentTimeMillis() - startTime > recordingTime;
      }
      out.close();
    } catch (IOException e){//TODO: handle exception
    }
    return out;
  }

  void playAudio (ByteArrayOutputStream out){
    try{
      byte audio[] = out.toByteArray();
      InputStream input = new ByteArrayInputStream(audio);
      AudioInputStream ais = new AudioInputStream(input, format, audio.length / format.getFrameSize());

      DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
      SourceDataLine line = (SourceDataLine)AudioSystem.getLine(info);
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

    } catch (IOException e){//TODO: handle exception
    } catch (LineUnavailableException e){ //TODO: handle exception
    }
  }

  String convertAudioToString(ByteArrayOutputStream stream) {
    byte[] bytes = stream.toByteArray();
    return new String(toCharArray(bytes));
  }

  ByteArrayOutputStream convertStringToByteArrayOutputStream(String input){
    byte[] output = toByteArray(input.toCharArray());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(output.length);
    outputStream.write(output,0,output.length);

    return outputStream;
  }

  char[] toCharArray (byte[] bytes){
    int length = bytes.length;
    char[] chars = new char[length];

    for (int i=0 ; i < length; i++){
      chars[i] = (char)(bytes[i] + 128);
    }

    return chars;
  }

  byte[] toByteArray (char[] chars) {
    int length = chars.length;
    byte[] bytes = new byte[length];
    for (int i=0 ; i < length; i++){
      bytes[i] =(byte)(chars[i]);
      bytes[i] -= 128;
    }
    return bytes;
  }

  public static void main (String[] args){
    QRPhone phone = new QRPhone();

    byte[] a = new byte[]{-1,2,3,4,22};
    byte[] b = phone.toByteArray(phone.toCharArray(a));


    ByteArrayOutputStream audio = phone.recordAudio(3000);
    String audiomessage = phone.convertAudioToString(audio);
    phone.playAudio(phone.convertStringToByteArrayOutputStream(audiomessage));
  }
}
