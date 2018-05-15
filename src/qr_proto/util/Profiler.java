package qr_proto.util;

import qr_proto.QRProtoSocket;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.HashMap;

public class Profiler {
  public static volatile boolean logMeasurements = true;

  private static volatile HashMap<String, Long> measurements = new HashMap<>();
  private static volatile Path logPath = Paths.get("profiler/profiler_" + QRProtoSocket.DISPLAY_TIME + "_" + QRProtoSocket.MAX_BUFFER_SIZE + "_" + LocalDate.now() + ".log");

  public static synchronized void startMeasurement(String name) {
    measurements.put(name, System.currentTimeMillis());
  }

  public static synchronized long endMeasurement(String name, boolean log) {
    boolean logTmp = logMeasurements;
    logMeasurements = log;
    long time = endMeasurement(name);
    logMeasurements = logTmp;
    return time;
  }

  public static synchronized long endMeasurement(String name) {
    if(!measurements.containsKey(name))
      throw new IllegalArgumentException("Measurement '" + name + "' not started.");

    long time = System.currentTimeMillis() - measurements.get(name);
    if(logMeasurements) {
      String msg = "Finished measurement '" + name + "' in " + time + "ms.";
      Log.outln(msg);
      profileData(name, time);
    }
    return time;
  }

  public static synchronized void profileData(String name, long data) {
    profileData(name, String.valueOf(data));
  }

  public static synchronized void profileData(String name, int data) {
    profileData(name, String.valueOf(data));
  }

  public static synchronized void profileData(String name, char data) {
    profileData(name, String.valueOf(data));
  }

  public static synchronized void profileData(String name, short data) {
    profileData(name, String.valueOf(data));
  }

  public static synchronized void profileData(String name, byte data) {
    profileData(name, String.valueOf(data));
  }

  public static synchronized void profileData(String name, float data) {
    profileData(name, String.valueOf(data));
  }

  public static synchronized void profileData(String name, double data) {
    profileData(name, String.valueOf(data));
  }

  public static synchronized void profileData(String name, String data) {
    String msg = "Profile data '" + name + "' with data " + data;
    Log.outln(msg);
    try {
      if(!Files.exists(logPath)) {
        Files.createFile(logPath);
        Files.write(logPath, ("name,data,display_time,max_size\n").getBytes(), StandardOpenOption.APPEND);
      }
      Files.write(logPath, (name + "," + data + "," + QRProtoSocket.DISPLAY_TIME + "," + QRProtoSocket.MAX_BUFFER_SIZE + "\n").getBytes(), StandardOpenOption.APPEND);
    } catch(IOException e) {
      e.printStackTrace();
    }
  }
}
