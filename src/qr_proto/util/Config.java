package qr_proto.util;

import qr_proto.qr.QRCode;

public class Config {
  public static final boolean COMPRESS = false;
  public static final int MAX_BUFFER_SIZE = (2953 - QRCode.METADATA_LENGTH - 1)/2; // don't ask
  public static final int DISPLAY_TIME = 200;
}
