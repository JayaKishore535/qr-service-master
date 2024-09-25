package com.dkg.qrservice.util;

public final class DKG_FormatExceptionDKG extends DKG_ReaderException {

  private static final DKG_FormatExceptionDKG INSTANCE = new DKG_FormatExceptionDKG();
  static {
    INSTANCE.setStackTrace(NO_TRACE); // since it's meaningless
  }

  private DKG_FormatExceptionDKG() {
  }

  private DKG_FormatExceptionDKG(Throwable cause) {
    super(cause);
  }

  public static DKG_FormatExceptionDKG getFormatInstance() {
    return isStackTrace ? new DKG_FormatExceptionDKG() : INSTANCE;
  }

  public static DKG_FormatExceptionDKG getFormatInstance(Throwable cause) {
    return isStackTrace ? new DKG_FormatExceptionDKG(cause) : INSTANCE;
  }
}
