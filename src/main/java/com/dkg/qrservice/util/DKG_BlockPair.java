package com.dkg.qrservice.util;

final class DKG_BlockPair {

  private final byte[] dataBytes;
  private final byte[] errorCorrectionBytes;

  DKG_BlockPair(byte[] data, byte[] errorCorrection) {
    dataBytes = data;
    errorCorrectionBytes = errorCorrection;
  }

  public byte[] getDataBytes() {
    return dataBytes;
  }

  public byte[] getErrorCorrectionBytes() {
    return errorCorrectionBytes;
  }

}
