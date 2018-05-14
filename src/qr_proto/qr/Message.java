package qr_proto.qr;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Message {
  public static final String MESSAGE_END = "\\0";
//  public static final boolean COMPRESS = true;

  private String message;
  private boolean complete, escaped;
  private DecimalFormat formatter;

  public Message(String message, boolean complete, boolean escaped) {
    this.message = message;
    this.complete = complete;
    this.escaped = escaped;
    this.formatter = new DecimalFormat("000000");
  }

  public String removeSubstring(int beginIndex, int endIndex) {
    String start = message.substring(beginIndex, endIndex);
    message = message.substring(endIndex);
    return start;
  }

  public Message escape (){
    if(!escaped) {
      byte[] toCompress;
      byte[] output = new byte[message.length()];
      Deflater compressor = new Deflater();

      try {
        toCompress = message.getBytes("UTF-8");
        compressor.setInput(toCompress);
        message = formatter.format(toCompress.length);
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }

      compressor.finish();
      compressor.deflate(output);
      compressor.end();

      char[] outputString = new char[output.length];
      for (int i=0; i < output.length; i++){
        outputString[i] = (char) (output[i] + 128);
      }

      message += new String(outputString);

      message = message.replace("\\", "\\b");
      if(complete)
        message += MESSAGE_END;
      escaped = true;
    }
    return this;
  }

  public Message unescape () {
    if(escaped) {

      int uncompressedDataLength = Integer.parseInt(message.substring(0,6));
      Inflater decompresser = new Inflater();
      char[] charInput = message.toCharArray();
      byte[] byteInput = new byte[charInput.length-6];

      for (int i=0; i < byteInput.length; i++){
        byteInput[i] = (byte) (charInput[i+6] + 128);
      }

      decompresser.setInput(byteInput, 0, byteInput.length);

      byte[] result = new byte[uncompressedDataLength];
      try {
        decompresser.inflate(result);
      } catch (DataFormatException e) {
        e.printStackTrace();
      }
      decompresser.end();

      try {
        message = new String(result, 0, uncompressedDataLength, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }

      int length = getMessageLength();
      if(complete)
        length -= MESSAGE_END.length();
      message = message.substring(0, length).replace("\\b", "\\");
      escaped = false;
    }
    return this;
  }

  public int getMessageLength() {
    return message.length();
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return getMessage();
  }
}
