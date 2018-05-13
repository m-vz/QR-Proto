package qr_proto.util;

import java.util.HashMap;

public class Profiler {
  public static volatile boolean logMeasurements = true;

  private static volatile HashMap<String, Long> measurements = new HashMap<>();

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
    if(logMeasurements)
      Log.outln("Finished measurement '" + name + "' in " + time + "ms.");
    return time;
  }
}
