package com.dkg.qrservice.util;

public abstract class DKG_ReaderException extends Exception {

  // disable stack traces when not running inside test units
  protected static boolean isStackTrace =
      System.getProperty("surefire.test.class.path") != null;
  protected static final StackTraceElement[] NO_TRACE = new StackTraceElement[0];

  DKG_ReaderException() {
    // do nothing
  }

  DKG_ReaderException(Throwable cause) {
    super(cause);
  }

  // Prevent stack traces from being taken
  @Override
  public final synchronized Throwable fillInStackTrace() {
    return null;
  }

  public static void setStackTrace(boolean enabled) {
    isStackTrace = enabled;
  }

}
