package com.dkg.qrservice.util;

final class DKG_FormatInformation {

  private static final int FORMAT_INFO_MASK_QR = 0x5412;

  /**
   * See ISO 18004:2006, Annex C, Table C.1
   */
  private static final int[][] FORMAT_INFO_DECODE_LOOKUP = {
      {0x5412, 0x00},
      {0x5125, 0x01},
      {0x5E7C, 0x02},
      {0x5B4B, 0x03},
      {0x45F9, 0x04},
      {0x40CE, 0x05},
      {0x4F97, 0x06},
      {0x4AA0, 0x07},
      {0x77C4, 0x08},
      {0x72F3, 0x09},
      {0x7DAA, 0x0A},
      {0x789D, 0x0B},
      {0x662F, 0x0C},
      {0x6318, 0x0D},
      {0x6C41, 0x0E},
      {0x6976, 0x0F},
      {0x1689, 0x10},
      {0x13BE, 0x11},
      {0x1CE7, 0x12},
      {0x19D0, 0x13},
      {0x0762, 0x14},
      {0x0255, 0x15},
      {0x0D0C, 0x16},
      {0x083B, 0x17},
      {0x355F, 0x18},
      {0x3068, 0x19},
      {0x3F31, 0x1A},
      {0x3A06, 0x1B},
      {0x24B4, 0x1C},
      {0x2183, 0x1D},
      {0x2EDA, 0x1E},
      {0x2BED, 0x1F},
  };

  private final DKG_ErrorCorrectionLevel DKGErrorCorrectionLevel;
  private final byte dataMask;

  private DKG_FormatInformation(int formatInfo) {
    // Bits 3,4
    DKGErrorCorrectionLevel = DKG_ErrorCorrectionLevel.forBits((formatInfo >> 3) & 0x03);
    // Bottom 3 bits
    dataMask = (byte) (formatInfo & 0x07);
  }

  static int numBitsDiffering(int a, int b) {
    return Integer.bitCount(a ^ b);
  }

  static DKG_FormatInformation decodeFormatInformation(int maskedFormatInfo1, int maskedFormatInfo2) {
    DKG_FormatInformation formatInfo = doDecodeFormatInformation(maskedFormatInfo1, maskedFormatInfo2);
    if (formatInfo != null) {
      return formatInfo;
    }
    // Should return null, but, some QR codes apparently
    // do not mask this info. Try again by actually masking the pattern
    // first
    return doDecodeFormatInformation(maskedFormatInfo1 ^ FORMAT_INFO_MASK_QR,
                                     maskedFormatInfo2 ^ FORMAT_INFO_MASK_QR);
  }

  private static DKG_FormatInformation doDecodeFormatInformation(int maskedFormatInfo1, int maskedFormatInfo2) {
    // Find the int in FORMAT_INFO_DECODE_LOOKUP with fewest bits differing
    int bestDifference = Integer.MAX_VALUE;
    int bestFormatInfo = 0;
    for (int[] decodeInfo : FORMAT_INFO_DECODE_LOOKUP) {
      int targetInfo = decodeInfo[0];
      if (targetInfo == maskedFormatInfo1 || targetInfo == maskedFormatInfo2) {
        // Found an exact match
        return new DKG_FormatInformation(decodeInfo[1]);
      }
      int bitsDifference = numBitsDiffering(maskedFormatInfo1, targetInfo);
      if (bitsDifference < bestDifference) {
        bestFormatInfo = decodeInfo[1];
        bestDifference = bitsDifference;
      }
      if (maskedFormatInfo1 != maskedFormatInfo2) {
        // also try the other option
        bitsDifference = numBitsDiffering(maskedFormatInfo2, targetInfo);
        if (bitsDifference < bestDifference) {
          bestFormatInfo = decodeInfo[1];
          bestDifference = bitsDifference;
        }
      }
    }
    // Hamming distance of the 32 masked codes is 7, by construction, so <= 3 bits
    // differing means we found a match
    if (bestDifference <= 3) {
      return new DKG_FormatInformation(bestFormatInfo);
    }
    return null;
  }

  DKG_ErrorCorrectionLevel getErrorCorrectionLevel() {
    return DKGErrorCorrectionLevel;
  }

  byte getDataMask() {
    return dataMask;
  }

  @Override
  public int hashCode() {
    return (DKGErrorCorrectionLevel.ordinal() << 3) | dataMask;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof DKG_FormatInformation)) {
      return false;
    }
    DKG_FormatInformation other = (DKG_FormatInformation) o;
    return this.DKGErrorCorrectionLevel == other.DKGErrorCorrectionLevel &&
        this.dataMask == other.dataMask;
  }

}
