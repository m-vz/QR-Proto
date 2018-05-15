package qr_proto.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static qr_proto.util.Config.COMPRESS;
import static qr_proto.util.Config.DISPLAY_TIME;
import static qr_proto.util.Config.MAX_BUFFER_SIZE;

public class Profiler {
  public static volatile boolean logMeasurements = true;

  private static volatile HashMap<String, Long> measurements = new HashMap<>();
  private static volatile Path logPath = Paths.get("profiler/profiler_" + DISPLAY_TIME + "_" + MAX_BUFFER_SIZE + "_" + (COMPRESS ? "compressed_" : "uncompressed_") + LocalDateTime.now() + ".csv");
  private static volatile HashMap<String, String> profileData = new HashMap<>();

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
    profileData.put(name, data);
  }

  public static synchronized void writeProfiledData() {
    StringBuilder logMsg = new StringBuilder("Profiled: "), header = new StringBuilder(), fileMsg = new StringBuilder();
    int c = 0;
    for(Map.Entry<String, String> data: profileData.entrySet()) {
      logMsg.append(data.getKey()).append(":").append(data.getValue());
      header.append("name,data,display_time,max_size");
      fileMsg.append(data.getKey() + "," + data.getValue() + "," + DISPLAY_TIME + "," + MAX_BUFFER_SIZE);
      if(c < profileData.size() - 1) {
        logMsg.append(",");
        header.append(",");
        fileMsg.append(",");
      }
      c++;
    }
    header.append("\n");
    fileMsg.append("\n");
    Log.outln(logMsg.toString());
    try {
      if(!Files.exists(logPath)) {
        Files.createFile(logPath);
        Files.write(logPath, header.toString().getBytes(), StandardOpenOption.APPEND);
      }
      Files.write(logPath, fileMsg.toString().getBytes(), StandardOpenOption.APPEND);
    } catch(IOException e) {
      e.printStackTrace();
    }
  }
}
