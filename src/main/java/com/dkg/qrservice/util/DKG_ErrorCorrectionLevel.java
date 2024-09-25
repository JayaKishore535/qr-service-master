package com.dkg.qrservice.util;

public enum DKG_ErrorCorrectionLevel {

  /** L = ~7% correction */
  L(0x01),
  /** M = ~15% correction */
  M(0x00),
  /** Q = ~25% correction */
  Q(0x03),
  /** H = ~30% correction */
  H(0x02);

  private static final DKG_ErrorCorrectionLevel[] FOR_BITS = {M, L, H, Q};

  private final int bits;

  DKG_ErrorCorrectionLevel(int bits) {
    this.bits = bits;
  }

  public int getBits() {
    return bits;
  }

  public static DKG_ErrorCorrectionLevel forBits(int bits) {
    if (bits < 0 || bits >= FOR_BITS.length) {
      throw new IllegalArgumentException();
    }
    return FOR_BITS[bits];
  }


}
