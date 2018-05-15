package qr_proto.util;

import qr_proto.qr.QRCode;

public class Config {
  public static final boolean COMPRESS = false;
  public static final int MAX_BUFFER_SIZE = (2953 - QRCode.METADATA_LENGTH - 1)/3; // don't ask
  public static final int DISPLAY_TIME = 200;
  public static final int NUMBER_OF_TEST_MESSAGES = 1;
  public static final int QR_CODE_SIZE = 1000;
}
