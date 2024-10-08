package com.dkg.qrservice.util;

public final class DKG_Version {

  private static final int[] VERSION_DECODE_INFO = {
      0x07C94, 0x085BC, 0x09A99, 0x0A4D3, 0x0BBF6,
      0x0C762, 0x0D847, 0x0E60D, 0x0F928, 0x10B78,
      0x1145D, 0x12A17, 0x13532, 0x149A6, 0x15683,
      0x168C9, 0x177EC, 0x18EC4, 0x191E1, 0x1AFAB,
      0x1B08E, 0x1CC1A, 0x1D33F, 0x1ED75, 0x1F250,
      0x209D5, 0x216F0, 0x228BA, 0x2379F, 0x24B0B,
      0x2542E, 0x26A64, 0x27541, 0x28C69
  };

  private static final DKG_Version[] DKG_VERSIONS = buildVersions();

  private final int versionNumber;
  private final int[] alignmentPatternCenters;
  private final ECBlocks[] ecBlocks;
  private final int totalCodewords;

  private DKG_Version(int versionNumber,
                      int[] alignmentPatternCenters,
                      ECBlocks... ecBlocks) {
    this.versionNumber = versionNumber;
    this.alignmentPatternCenters = alignmentPatternCenters;
    this.ecBlocks = ecBlocks;
    int total = 0;
    int ecCodewords = ecBlocks[0].getECCodewordsPerBlock();
    ECB[] ecbArray = ecBlocks[0].getECBlocks();
    for (ECB ecBlock : ecbArray) {
      total += ecBlock.getCount() * (ecBlock.getDataCodewords() + ecCodewords);
    }
    this.totalCodewords = total;
  }

  public int getVersionNumber() {
    return versionNumber;
  }

  public int[] getAlignmentPatternCenters() {
    return alignmentPatternCenters;
  }

  public int getTotalCodewords() {
    return totalCodewords;
  }

  public int getDimensionForVersion() {
    return 17 + 4 * versionNumber;
  }

  public ECBlocks getECBlocksForLevel(DKG_ErrorCorrectionLevel ecLevel) {
    return ecBlocks[ecLevel.ordinal()];
  }


  public static DKG_Version getProvisionalVersionForDimension(int dimension) throws DKG_FormatExceptionDKG {
    if (dimension % 4 != 1) {
      throw DKG_FormatExceptionDKG.getFormatInstance();
    }
    try {
      return getVersionForNumber((dimension - 17) / 4);
    } catch (IllegalArgumentException ignored) {
      throw DKG_FormatExceptionDKG.getFormatInstance();
    }
  }

  public static DKG_Version getVersionForNumber(int versionNumber) {
    if (versionNumber < 1 || versionNumber > 40) {
      throw new IllegalArgumentException();
    }
    return DKG_VERSIONS[versionNumber - 1];
  }

  static DKG_Version decodeVersionInformation(int versionBits) {
    int bestDifference = Integer.MAX_VALUE;
    int bestVersion = 0;
    for (int i = 0; i < VERSION_DECODE_INFO.length; i++) {
      int targetVersion = VERSION_DECODE_INFO[i];
      // Do the version info bits match exactly? done.
      if (targetVersion == versionBits) {
        return getVersionForNumber(i + 7);
      }
      // Otherwise see if this is the closest to a real version info bit string
      // we have seen so far
      int bitsDifference = DKG_FormatInformation.numBitsDiffering(versionBits, targetVersion);
      if (bitsDifference < bestDifference) {
        bestVersion = i + 7;
        bestDifference = bitsDifference;
      }
    }
    // We can tolerate up to 3 bits of error since no two version info codewords will
    // differ in less than 8 bits.
    if (bestDifference <= 3) {
      return getVersionForNumber(bestVersion);
    }
    // If we didn't find a close enough match, fail
    return null;
  }

  /**
   * See ISO 18004:2006 Annex E
   */
  DKG_BitMatrix buildFunctionPattern() {
    int dimension = getDimensionForVersion();
    DKG_BitMatrix DKGBitMatrix = new DKG_BitMatrix(dimension);

    // Top left finder pattern + separator + format
    DKGBitMatrix.setRegion(0, 0, 9, 9);
    // Top right finder pattern + separator + format
    DKGBitMatrix.setRegion(dimension - 8, 0, 8, 9);
    // Bottom left finder pattern + separator + format
    DKGBitMatrix.setRegion(0, dimension - 8, 9, 8);

    // Alignment patterns
    int max = alignmentPatternCenters.length;
    for (int x = 0; x < max; x++) {
      int i = alignmentPatternCenters[x] - 2;
      for (int y = 0; y < max; y++) {
        if ((x != 0 || (y != 0 && y != max - 1)) && (x != max - 1 || y != 0)) {
          DKGBitMatrix.setRegion(alignmentPatternCenters[y] - 2, i, 5, 5);
        }
        // else no o alignment patterns near the three finder patterns
      }
    }

    // Vertical timing pattern
    DKGBitMatrix.setRegion(6, 9, 1, dimension - 17);
    // Horizontal timing pattern
    DKGBitMatrix.setRegion(9, 6, dimension - 17, 1);

    if (versionNumber > 6) {
      // Version info, top right
      DKGBitMatrix.setRegion(dimension - 11, 0, 3, 6);
      // Version info, bottom left
      DKGBitMatrix.setRegion(0, dimension - 11, 6, 3);
    }

    return DKGBitMatrix;
  }

  public static final class ECBlocks {
    private final int ecCodewordsPerBlock;
    private final ECB[] ecBlocks;

    ECBlocks(int ecCodewordsPerBlock, ECB... ecBlocks) {
      this.ecCodewordsPerBlock = ecCodewordsPerBlock;
      this.ecBlocks = ecBlocks;
    }

    public int getECCodewordsPerBlock() {
      return ecCodewordsPerBlock;
    }

    public int getNumBlocks() {
      int total = 0;
      for (ECB ecBlock : ecBlocks) {
        total += ecBlock.getCount();
      }
      return total;
    }

    public int getTotalECCodewords() {
      return ecCodewordsPerBlock * getNumBlocks();
    }

    public ECB[] getECBlocks() {
      return ecBlocks;
    }
  }

  public static final class ECB {
    private final int count;
    private final int dataCodewords;

    ECB(int count, int dataCodewords) {
      this.count = count;
      this.dataCodewords = dataCodewords;
    }

    public int getCount() {
      return count;
    }

    public int getDataCodewords() {
      return dataCodewords;
    }
  }

  @Override
  public String toString() {
    return String.valueOf(versionNumber);
  }


  private static DKG_Version[] buildVersions() {
    return new DKG_Version[]{
        new DKG_Version(1, new int[]{},
            new ECBlocks(7, new ECB(1, 19)),
            new ECBlocks(10, new ECB(1, 16)),
            new ECBlocks(13, new ECB(1, 13)),
            new ECBlocks(17, new ECB(1, 9))),
        new DKG_Version(2, new int[]{6, 18},
            new ECBlocks(10, new ECB(1, 34)),
            new ECBlocks(16, new ECB(1, 28)),
            new ECBlocks(22, new ECB(1, 22)),
            new ECBlocks(28, new ECB(1, 16))),
        new DKG_Version(3, new int[]{6, 22},
            new ECBlocks(15, new ECB(1, 55)),
            new ECBlocks(26, new ECB(1, 44)),
            new ECBlocks(18, new ECB(2, 17)),
            new ECBlocks(22, new ECB(2, 13))),
        new DKG_Version(4, new int[]{6, 26},
            new ECBlocks(20, new ECB(1, 80)),
            new ECBlocks(18, new ECB(2, 32)),
            new ECBlocks(26, new ECB(2, 24)),
            new ECBlocks(16, new ECB(4, 9))),
        new DKG_Version(5, new int[]{6, 30},
            new ECBlocks(26, new ECB(1, 108)),
            new ECBlocks(24, new ECB(2, 43)),
            new ECBlocks(18, new ECB(2, 15),
                new ECB(2, 16)),
            new ECBlocks(22, new ECB(2, 11),
                new ECB(2, 12))),
        new DKG_Version(6, new int[]{6, 34},
            new ECBlocks(18, new ECB(2, 68)),
            new ECBlocks(16, new ECB(4, 27)),
            new ECBlocks(24, new ECB(4, 19)),
            new ECBlocks(28, new ECB(4, 15))),
        new DKG_Version(7, new int[]{6, 22, 38},
            new ECBlocks(20, new ECB(2, 78)),
            new ECBlocks(18, new ECB(4, 31)),
            new ECBlocks(18, new ECB(2, 14),
                new ECB(4, 15)),
            new ECBlocks(26, new ECB(4, 13),
                new ECB(1, 14))),
        new DKG_Version(8, new int[]{6, 24, 42},
            new ECBlocks(24, new ECB(2, 97)),
            new ECBlocks(22, new ECB(2, 38),
                new ECB(2, 39)),
            new ECBlocks(22, new ECB(4, 18),
                new ECB(2, 19)),
            new ECBlocks(26, new ECB(4, 14),
                new ECB(2, 15))),
        new DKG_Version(9, new int[]{6, 26, 46},
            new ECBlocks(30, new ECB(2, 116)),
            new ECBlocks(22, new ECB(3, 36),
                new ECB(2, 37)),
            new ECBlocks(20, new ECB(4, 16),
                new ECB(4, 17)),
            new ECBlocks(24, new ECB(4, 12),
                new ECB(4, 13))),
        new DKG_Version(10, new int[]{6, 28, 50},
            new ECBlocks(18, new ECB(2, 68),
                new ECB(2, 69)),
            new ECBlocks(26, new ECB(4, 43),
                new ECB(1, 44)),
            new ECBlocks(24, new ECB(6, 19),
                new ECB(2, 20)),
            new ECBlocks(28, new ECB(6, 15),
                new ECB(2, 16))),
        new DKG_Version(11, new int[]{6, 30, 54},
            new ECBlocks(20, new ECB(4, 81)),
            new ECBlocks(30, new ECB(1, 50),
                new ECB(4, 51)),
            new ECBlocks(28, new ECB(4, 22),
                new ECB(4, 23)),
            new ECBlocks(24, new ECB(3, 12),
                new ECB(8, 13))),
        new DKG_Version(12, new int[]{6, 32, 58},
            new ECBlocks(24, new ECB(2, 92),
                new ECB(2, 93)),
            new ECBlocks(22, new ECB(6, 36),
                new ECB(2, 37)),
            new ECBlocks(26, new ECB(4, 20),
                new ECB(6, 21)),
            new ECBlocks(28, new ECB(7, 14),
                new ECB(4, 15))),
        new DKG_Version(13, new int[]{6, 34, 62},
            new ECBlocks(26, new ECB(4, 107)),
            new ECBlocks(22, new ECB(8, 37),
                new ECB(1, 38)),
            new ECBlocks(24, new ECB(8, 20),
                new ECB(4, 21)),
            new ECBlocks(22, new ECB(12, 11),
                new ECB(4, 12))),
        new DKG_Version(14, new int[]{6, 26, 46, 66},
            new ECBlocks(30, new ECB(3, 115),
                new ECB(1, 116)),
            new ECBlocks(24, new ECB(4, 40),
                new ECB(5, 41)),
            new ECBlocks(20, new ECB(11, 16),
                new ECB(5, 17)),
            new ECBlocks(24, new ECB(11, 12),
                new ECB(5, 13))),
        new DKG_Version(15, new int[]{6, 26, 48, 70},
            new ECBlocks(22, new ECB(5, 87),
                new ECB(1, 88)),
            new ECBlocks(24, new ECB(5, 41),
                new ECB(5, 42)),
            new ECBlocks(30, new ECB(5, 24),
                new ECB(7, 25)),
            new ECBlocks(24, new ECB(11, 12),
                new ECB(7, 13))),
        new DKG_Version(16, new int[]{6, 26, 50, 74},
            new ECBlocks(24, new ECB(5, 98),
                new ECB(1, 99)),
            new ECBlocks(28, new ECB(7, 45),
                new ECB(3, 46)),
            new ECBlocks(24, new ECB(15, 19),
                new ECB(2, 20)),
            new ECBlocks(30, new ECB(3, 15),
                new ECB(13, 16))),
        new DKG_Version(17, new int[]{6, 30, 54, 78},
            new ECBlocks(28, new ECB(1, 107),
                new ECB(5, 108)),
            new ECBlocks(28, new ECB(10, 46),
                new ECB(1, 47)),
            new ECBlocks(28, new ECB(1, 22),
                new ECB(15, 23)),
            new ECBlocks(28, new ECB(2, 14),
                new ECB(17, 15))),
        new DKG_Version(18, new int[]{6, 30, 56, 82},
            new ECBlocks(30, new ECB(5, 120),
                new ECB(1, 121)),
            new ECBlocks(26, new ECB(9, 43),
                new ECB(4, 44)),
            new ECBlocks(28, new ECB(17, 22),
                new ECB(1, 23)),
            new ECBlocks(28, new ECB(2, 14),
                new ECB(19, 15))),
        new DKG_Version(19, new int[]{6, 30, 58, 86},
            new ECBlocks(28, new ECB(3, 113),
                new ECB(4, 114)),
            new ECBlocks(26, new ECB(3, 44),
                new ECB(11, 45)),
            new ECBlocks(26, new ECB(17, 21),
                new ECB(4, 22)),
            new ECBlocks(26, new ECB(9, 13),
                new ECB(16, 14))),
        new DKG_Version(20, new int[]{6, 34, 62, 90},
            new ECBlocks(28, new ECB(3, 107),
                new ECB(5, 108)),
            new ECBlocks(26, new ECB(3, 41),
                new ECB(13, 42)),
            new ECBlocks(30, new ECB(15, 24),
                new ECB(5, 25)),
            new ECBlocks(28, new ECB(15, 15),
                new ECB(10, 16))),
        new DKG_Version(21, new int[]{6, 28, 50, 72, 94},
            new ECBlocks(28, new ECB(4, 116),
                new ECB(4, 117)),
            new ECBlocks(26, new ECB(17, 42)),
            new ECBlocks(28, new ECB(17, 22),
                new ECB(6, 23)),
            new ECBlocks(30, new ECB(19, 16),
                new ECB(6, 17))),
        new DKG_Version(22, new int[]{6, 26, 50, 74, 98},
            new ECBlocks(28, new ECB(2, 111),
                new ECB(7, 112)),
            new ECBlocks(28, new ECB(17, 46)),
            new ECBlocks(30, new ECB(7, 24),
                new ECB(16, 25)),
            new ECBlocks(24, new ECB(34, 13))),
        new DKG_Version(23, new int[]{6, 30, 54, 78, 102},
            new ECBlocks(30, new ECB(4, 121),
                new ECB(5, 122)),
            new ECBlocks(28, new ECB(4, 47),
                new ECB(14, 48)),
            new ECBlocks(30, new ECB(11, 24),
                new ECB(14, 25)),
            new ECBlocks(30, new ECB(16, 15),
                new ECB(14, 16))),
        new DKG_Version(24, new int[]{6, 28, 54, 80, 106},
            new ECBlocks(30, new ECB(6, 117),
                new ECB(4, 118)),
            new ECBlocks(28, new ECB(6, 45),
                new ECB(14, 46)),
            new ECBlocks(30, new ECB(11, 24),
                new ECB(16, 25)),
            new ECBlocks(30, new ECB(30, 16),
                new ECB(2, 17))),
        new DKG_Version(25, new int[]{6, 32, 58, 84, 110},
            new ECBlocks(26, new ECB(8, 106),
                new ECB(4, 107)),
            new ECBlocks(28, new ECB(8, 47),
                new ECB(13, 48)),
            new ECBlocks(30, new ECB(7, 24),
                new ECB(22, 25)),
            new ECBlocks(30, new ECB(22, 15),
                new ECB(13, 16))),
        new DKG_Version(26, new int[]{6, 30, 58, 86, 114},
            new ECBlocks(28, new ECB(10, 114),
                new ECB(2, 115)),
            new ECBlocks(28, new ECB(19, 46),
                new ECB(4, 47)),
            new ECBlocks(28, new ECB(28, 22),
                new ECB(6, 23)),
            new ECBlocks(30, new ECB(33, 16),
                new ECB(4, 17))),
        new DKG_Version(27, new int[]{6, 34, 62, 90, 118},
            new ECBlocks(30, new ECB(8, 122),
                new ECB(4, 123)),
            new ECBlocks(28, new ECB(22, 45),
                new ECB(3, 46)),
            new ECBlocks(30, new ECB(8, 23),
                new ECB(26, 24)),
            new ECBlocks(30, new ECB(12, 15),
                new ECB(28, 16))),
        new DKG_Version(28, new int[]{6, 26, 50, 74, 98, 122},
            new ECBlocks(30, new ECB(3, 117),
                new ECB(10, 118)),
            new ECBlocks(28, new ECB(3, 45),
                new ECB(23, 46)),
            new ECBlocks(30, new ECB(4, 24),
                new ECB(31, 25)),
            new ECBlocks(30, new ECB(11, 15),
                new ECB(31, 16))),
        new DKG_Version(29, new int[]{6, 30, 54, 78, 102, 126},
            new ECBlocks(30, new ECB(7, 116),
                new ECB(7, 117)),
            new ECBlocks(28, new ECB(21, 45),
                new ECB(7, 46)),
            new ECBlocks(30, new ECB(1, 23),
                new ECB(37, 24)),
            new ECBlocks(30, new ECB(19, 15),
                new ECB(26, 16))),
        new DKG_Version(30, new int[]{6, 26, 52, 78, 104, 130},
            new ECBlocks(30, new ECB(5, 115),
                new ECB(10, 116)),
            new ECBlocks(28, new ECB(19, 47),
                new ECB(10, 48)),
            new ECBlocks(30, new ECB(15, 24),
                new ECB(25, 25)),
            new ECBlocks(30, new ECB(23, 15),
                new ECB(25, 16))),
        new DKG_Version(31, new int[]{6, 30, 56, 82, 108, 134},
            new ECBlocks(30, new ECB(13, 115),
                new ECB(3, 116)),
            new ECBlocks(28, new ECB(2, 46),
                new ECB(29, 47)),
            new ECBlocks(30, new ECB(42, 24),
                new ECB(1, 25)),
            new ECBlocks(30, new ECB(23, 15),
                new ECB(28, 16))),
        new DKG_Version(32, new int[]{6, 34, 60, 86, 112, 138},
            new ECBlocks(30, new ECB(17, 115)),
            new ECBlocks(28, new ECB(10, 46),
                new ECB(23, 47)),
            new ECBlocks(30, new ECB(10, 24),
                new ECB(35, 25)),
            new ECBlocks(30, new ECB(19, 15),
                new ECB(35, 16))),
        new DKG_Version(33, new int[]{6, 30, 58, 86, 114, 142},
            new ECBlocks(30, new ECB(17, 115),
                new ECB(1, 116)),
            new ECBlocks(28, new ECB(14, 46),
                new ECB(21, 47)),
            new ECBlocks(30, new ECB(29, 24),
                new ECB(19, 25)),
            new ECBlocks(30, new ECB(11, 15),
                new ECB(46, 16))),
        new DKG_Version(34, new int[]{6, 34, 62, 90, 118, 146},
            new ECBlocks(30, new ECB(13, 115),
                new ECB(6, 116)),
            new ECBlocks(28, new ECB(14, 46),
                new ECB(23, 47)),
            new ECBlocks(30, new ECB(44, 24),
                new ECB(7, 25)),
            new ECBlocks(30, new ECB(59, 16),
                new ECB(1, 17))),
        new DKG_Version(35, new int[]{6, 30, 54, 78, 102, 126, 150},
            new ECBlocks(30, new ECB(12, 121),
                new ECB(7, 122)),
            new ECBlocks(28, new ECB(12, 47),
                new ECB(26, 48)),
            new ECBlocks(30, new ECB(39, 24),
                new ECB(14, 25)),
            new ECBlocks(30, new ECB(22, 15),
                new ECB(41, 16))),
        new DKG_Version(36, new int[]{6, 24, 50, 76, 102, 128, 154},
            new ECBlocks(30, new ECB(6, 121),
                new ECB(14, 122)),
            new ECBlocks(28, new ECB(6, 47),
                new ECB(34, 48)),
            new ECBlocks(30, new ECB(46, 24),
                new ECB(10, 25)),
            new ECBlocks(30, new ECB(2, 15),
                new ECB(64, 16))),
        new DKG_Version(37, new int[]{6, 28, 54, 80, 106, 132, 158},
            new ECBlocks(30, new ECB(17, 122),
                new ECB(4, 123)),
            new ECBlocks(28, new ECB(29, 46),
                new ECB(14, 47)),
            new ECBlocks(30, new ECB(49, 24),
                new ECB(10, 25)),
            new ECBlocks(30, new ECB(24, 15),
                new ECB(46, 16))),
        new DKG_Version(38, new int[]{6, 32, 58, 84, 110, 136, 162},
            new ECBlocks(30, new ECB(4, 122),
                new ECB(18, 123)),
            new ECBlocks(28, new ECB(13, 46),
                new ECB(32, 47)),
            new ECBlocks(30, new ECB(48, 24),
                new ECB(14, 25)),
            new ECBlocks(30, new ECB(42, 15),
                new ECB(32, 16))),
        new DKG_Version(39, new int[]{6, 26, 54, 82, 110, 138, 166},
            new ECBlocks(30, new ECB(20, 117),
                new ECB(4, 118)),
            new ECBlocks(28, new ECB(40, 47),
                new ECB(7, 48)),
            new ECBlocks(30, new ECB(43, 24),
                new ECB(22, 25)),
            new ECBlocks(30, new ECB(10, 15),
                new ECB(67, 16))),
        new DKG_Version(40, new int[]{6, 30, 58, 86, 114, 142, 170},
            new ECBlocks(30, new ECB(19, 118),
                new ECB(6, 119)),
            new ECBlocks(28, new ECB(18, 47),
                new ECB(31, 48)),
            new ECBlocks(30, new ECB(34, 24),
                new ECB(34, 25)),
            new ECBlocks(30, new ECB(20, 15),
                new ECB(61, 16)))
    };
  }

}
