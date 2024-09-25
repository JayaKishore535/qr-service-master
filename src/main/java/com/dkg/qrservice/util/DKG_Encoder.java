package com.dkg.qrservice.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;


public final class DKG_Encoder {

  // The original table is defined in the table 5 of JISX0510:2004 (p.19).
  private static final int[] ALPHANUMERIC_TABLE = {
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  // 0x00-0x0f
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  // 0x10-0x1f
      36, -1, -1, -1, 37, 38, -1, -1, -1, -1, 39, 40, -1, 41, 42, 43,  // 0x20-0x2f
      0,   1,  2,  3,  4,  5,  6,  7,  8,  9, 44, -1, -1, -1, -1, -1,  // 0x30-0x3f
      -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,  // 0x40-0x4f
      25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1,  // 0x50-0x5f
  };

  static final Charset DEFAULT_BYTE_MODE_ENCODING = StandardCharsets.ISO_8859_1;

  private DKG_Encoder() {
  }

  // The mask penalty calculation is complicated.  See Table 21 of JISX0510:2004 (p.45) for details.
  // Basically it applies four rules and summate all penalties.
  private static int calculateMaskPenalty(DKG_ByteMatrix matrix) {
    return DKG_MaskUtil.applyMaskPenaltyRule1(matrix)
        + DKG_MaskUtil.applyMaskPenaltyRule2(matrix)
        + DKG_MaskUtil.applyMaskPenaltyRule3(matrix)
        + DKG_MaskUtil.applyMaskPenaltyRule4(matrix);
  }


  public static DKG_QRCode encode(String content, DKG_ErrorCorrectionLevel ecLevel) throws DKG_WriterException {
    return encode(content, ecLevel, null);
  }

  public static DKG_QRCode encode(String content,
                                  DKG_ErrorCorrectionLevel ecLevel,
                                  Map<DKG_EncodeHintType,?> hints) throws DKG_WriterException {

    DKG_Version DKGVersion;
    DKG_BitArray headerAndDataBits;
    DKG_QR_Mode QRMode;

    boolean hasGS1FormatHint = hints != null && hints.containsKey(DKG_EncodeHintType.GS1_FORMAT) &&
        Boolean.parseBoolean(hints.get(DKG_EncodeHintType.GS1_FORMAT).toString());
    boolean hasCompactionHint = hints != null && hints.containsKey(DKG_EncodeHintType.QR_COMPACT) &&
        Boolean.parseBoolean(hints.get(DKG_EncodeHintType.QR_COMPACT).toString());

    // Determine what character encoding has been specified by the caller, if any
    Charset encoding = DEFAULT_BYTE_MODE_ENCODING;
    boolean hasEncodingHint = hints != null && hints.containsKey(DKG_EncodeHintType.CHARACTER_SET);
    if (hasEncodingHint) {
      encoding = Charset.forName(hints.get(DKG_EncodeHintType.CHARACTER_SET).toString());
    }

    if (hasCompactionHint) {
      QRMode = DKG_QR_Mode.BYTE;

      Charset priorityEncoding = encoding.equals(DEFAULT_BYTE_MODE_ENCODING) ? null : encoding;
      DKG_MinimalEncoder.ResultList rn = DKG_MinimalEncoder.encode(content, null, priorityEncoding, hasGS1FormatHint, ecLevel);

      headerAndDataBits = new DKG_BitArray();
      rn.getBits(headerAndDataBits);
      DKGVersion = rn.getVersion();

    } else {
    
      // Pick an encoding mode appropriate for the content. Note that this will not attempt to use
      // multiple modes / segments even if that were more efficient.
      QRMode = chooseMode(content, encoding);
  
      // This will store the header information, like mode and
      // length, as well as "header" segments like an ECI segment.
      DKG_BitArray headerBits = new DKG_BitArray();
  
      // Append ECI segment if applicable
      if (QRMode == DKG_QR_Mode.BYTE && hasEncodingHint) {
        DKG_CharacterSetECI eci = DKG_CharacterSetECI.getCharacterSetECI(encoding);
        if (eci != null) {
          appendECI(eci, headerBits);
        }
      }
  
      // Append the FNC1 mode header for GS1 formatted data if applicable
      if (hasGS1FormatHint) {
        // GS1 formatted codes are prefixed with a FNC1 in first position mode header
        appendModeInfo(DKG_QR_Mode.FNC1_FIRST_POSITION, headerBits);
      }
    
      // (With ECI in place,) Write the mode marker
      appendModeInfo(QRMode, headerBits);
  
      // Collect data within the main segment, separately, to count its size if needed. Don't add it to
      // main payload yet.
      DKG_BitArray dataBits = new DKG_BitArray();
      appendBytes(content, QRMode, dataBits, encoding);
  
      if (hints != null && hints.containsKey(DKG_EncodeHintType.QR_VERSION)) {
        int versionNumber = Integer.parseInt(hints.get(DKG_EncodeHintType.QR_VERSION).toString());
        DKGVersion = com.dkg.qrservice.util.DKG_Version.getVersionForNumber(versionNumber);
        int bitsNeeded = calculateBitsNeeded(QRMode, headerBits, dataBits, DKGVersion);
        if (!willFit(bitsNeeded, DKGVersion, ecLevel)) {
          throw new DKG_WriterException("Data too big for requested version");
        }
      } else {
        DKGVersion = recommendVersion(ecLevel, QRMode, headerBits, dataBits);
      }
    
      headerAndDataBits = new DKG_BitArray();
      headerAndDataBits.appendBitArray(headerBits);
      // Find "length" of main segment and write it
      int numLetters = QRMode == DKG_QR_Mode.BYTE ? dataBits.getSizeInBytes() : content.length();
      appendLengthInfo(numLetters, DKGVersion, QRMode, headerAndDataBits);
      // Put data together into the overall payload
      headerAndDataBits.appendBitArray(dataBits);
    }

    DKG_Version.ECBlocks ecBlocks = DKGVersion.getECBlocksForLevel(ecLevel);
    int numDataBytes = DKGVersion.getTotalCodewords() - ecBlocks.getTotalECCodewords();

    // Terminate the bits properly.
    terminateBits(numDataBytes, headerAndDataBits);

    // Interleave data bits with error correction code.
    DKG_BitArray finalBits = interleaveWithECBytes(headerAndDataBits,
                                               DKGVersion.getTotalCodewords(),
                                               numDataBytes,
                                               ecBlocks.getNumBlocks());

    DKG_QRCode DKGQrCode = new DKG_QRCode();

    DKGQrCode.setECLevel(ecLevel);
    DKGQrCode.setMode(QRMode);
    DKGQrCode.setVersion(DKGVersion);

    //  Choose the mask pattern and set to "qrCode".
    int dimension = DKGVersion.getDimensionForVersion();
    DKG_ByteMatrix matrix = new DKG_ByteMatrix(dimension, dimension);

    // Enable manual selection of the pattern to be used via hint
    int maskPattern = -1;
    if (hints != null && hints.containsKey(DKG_EncodeHintType.QR_MASK_PATTERN)) {
      int hintMaskPattern = Integer.parseInt(hints.get(DKG_EncodeHintType.QR_MASK_PATTERN).toString());
      maskPattern = DKG_QRCode.isValidMaskPattern(hintMaskPattern) ? hintMaskPattern : -1;
    }

    if (maskPattern == -1) {
      maskPattern = chooseMaskPattern(finalBits, ecLevel, DKGVersion, matrix);
    }
    DKGQrCode.setMaskPattern(maskPattern);

    // Build the matrix and set it to "qrCode".
    DKG_MatrixUtil.buildMatrix(finalBits, ecLevel, DKGVersion, maskPattern, matrix);
    DKGQrCode.setMatrix(matrix);

    return DKGQrCode;
  }


  private static DKG_Version recommendVersion(DKG_ErrorCorrectionLevel ecLevel,
                                              DKG_QR_Mode QRMode,
                                              DKG_BitArray headerBits,
                                              DKG_BitArray dataBits) throws DKG_WriterException {
    // Hard part: need to know version to know how many bits length takes. But need to know how many
    // bits it takes to know version. First we take a guess at version by assuming version will be
    // the minimum, 1:
    int provisionalBitsNeeded = calculateBitsNeeded(QRMode, headerBits, dataBits, com.dkg.qrservice.util.DKG_Version.getVersionForNumber(1));
    DKG_Version provisionalDKGVersion = chooseVersion(provisionalBitsNeeded, ecLevel);

    // Use that guess to calculate the right version. I am still not sure this works in 100% of cases.
    int bitsNeeded = calculateBitsNeeded(QRMode, headerBits, dataBits, provisionalDKGVersion);
    return chooseVersion(bitsNeeded, ecLevel);
  }

  private static int calculateBitsNeeded(DKG_QR_Mode QRMode,
                                         DKG_BitArray headerBits,
                                         DKG_BitArray dataBits,
                                         DKG_Version DKGVersion) {
    return headerBits.getSize() + QRMode.getCharacterCountInBits(DKGVersion) + dataBits.getSize();
  }

  static int getAlphanumericCode(int code) {
    if (code < ALPHANUMERIC_TABLE.length) {
      return ALPHANUMERIC_TABLE[code];
    }
    return -1;
  }

  public static DKG_QR_Mode chooseMode(String content) {
    return chooseMode(content, null);
  }


  private static DKG_QR_Mode chooseMode(String content, Charset encoding) {
    if (DKG_StringUtils.SHIFT_JIS_CHARSET.equals(encoding) && isOnlyDoubleByteKanji(content)) {
      // Choose Kanji mode if all input are double-byte characters
      return DKG_QR_Mode.KANJI;
    }
    boolean hasNumeric = false;
    boolean hasAlphanumeric = false;
    for (int i = 0; i < content.length(); ++i) {
      char c = content.charAt(i);
      if (c >= '0' && c <= '9') {
        hasNumeric = true;
      } else if (getAlphanumericCode(c) != -1) {
        hasAlphanumeric = true;
      } else {
        return DKG_QR_Mode.BYTE;
      }
    }
    if (hasAlphanumeric) {
      return DKG_QR_Mode.ALPHANUMERIC;
    }
    if (hasNumeric) {
      return DKG_QR_Mode.NUMERIC;
    }
    return DKG_QR_Mode.BYTE;
  }

  static boolean isOnlyDoubleByteKanji(String content) {
    byte[] bytes = content.getBytes(DKG_StringUtils.SHIFT_JIS_CHARSET);
    int length = bytes.length;
    if (length % 2 != 0) {
      return false;
    }
    for (int i = 0; i < length; i += 2) {
      int byte1 = bytes[i] & 0xFF;
      if ((byte1 < 0x81 || byte1 > 0x9F) && (byte1 < 0xE0 || byte1 > 0xEB)) {
        return false;
      }
    }
    return true;
  }

  private static int chooseMaskPattern(DKG_BitArray bits,
                                       DKG_ErrorCorrectionLevel ecLevel,
                                       DKG_Version DKGVersion,
                                       DKG_ByteMatrix matrix) throws DKG_WriterException {

    int minPenalty = Integer.MAX_VALUE;  // Lower penalty is better.
    int bestMaskPattern = -1;
    // We try all mask patterns to choose the best one.
    for (int maskPattern = 0; maskPattern < DKG_QRCode.NUM_MASK_PATTERNS; maskPattern++) {
      DKG_MatrixUtil.buildMatrix(bits, ecLevel, DKGVersion, maskPattern, matrix);
      int penalty = calculateMaskPenalty(matrix);
      if (penalty < minPenalty) {
        minPenalty = penalty;
        bestMaskPattern = maskPattern;
      }
    }
    return bestMaskPattern;
  }

  private static DKG_Version chooseVersion(int numInputBits, DKG_ErrorCorrectionLevel ecLevel) throws DKG_WriterException {
    for (int versionNum = 1; versionNum <= 40; versionNum++) {
      DKG_Version DKGVersion = com.dkg.qrservice.util.DKG_Version.getVersionForNumber(versionNum);
      if (willFit(numInputBits, DKGVersion, ecLevel)) {
        return DKGVersion;
      }
    }
    throw new DKG_WriterException("Data too big");
  }


  static boolean willFit(int numInputBits, DKG_Version DKGVersion, DKG_ErrorCorrectionLevel ecLevel) {
    // In the following comments, we use numbers of Version 7-H.
    // numBytes = 196
    int numBytes = DKGVersion.getTotalCodewords();
    // getNumECBytes = 130
    DKG_Version.ECBlocks ecBlocks = DKGVersion.getECBlocksForLevel(ecLevel);
    int numEcBytes = ecBlocks.getTotalECCodewords();
    // getNumDataBytes = 196 - 130 = 66
    int numDataBytes = numBytes - numEcBytes;
    int totalInputBytes = (numInputBits + 7) / 8;
    return numDataBytes >= totalInputBytes;
  }


  static void terminateBits(int numDataBytes, DKG_BitArray bits) throws DKG_WriterException {
    int capacity = numDataBytes * 8;
    if (bits.getSize() > capacity) {
      throw new DKG_WriterException("data bits cannot fit in the QR Code" + bits.getSize() + " > " +
          capacity);
    }
    // Append Mode.TERMINATE if there is enough space (value is 0000)
    for (int i = 0; i < 4 && bits.getSize() < capacity; ++i) {
      bits.appendBit(false);
    }
    // Append termination bits. See 8.4.8 of JISX0510:2004 (p.24) for details.
    // If the last byte isn't 8-bit aligned, we'll add padding bits.
    int numBitsInLastByte = bits.getSize() & 0x07;
    if (numBitsInLastByte > 0) {
      for (int i = numBitsInLastByte; i < 8; i++) {
        bits.appendBit(false);
      }
    }
    // If we have more space, we'll fill the space with padding patterns defined in 8.4.9 (p.24).
    int numPaddingBytes = numDataBytes - bits.getSizeInBytes();
    for (int i = 0; i < numPaddingBytes; ++i) {
      bits.appendBits((i & 0x01) == 0 ? 0xEC : 0x11, 8);
    }
    if (bits.getSize() != capacity) {
      throw new DKG_WriterException("Bits size does not equal capacity");
    }
  }


  static void getNumDataBytesAndNumECBytesForBlockID(int numTotalBytes,
                                                     int numDataBytes,
                                                     int numRSBlocks,
                                                     int blockID,
                                                     int[] numDataBytesInBlock,
                                                     int[] numECBytesInBlock) throws DKG_WriterException {
    if (blockID >= numRSBlocks) {
      throw new DKG_WriterException("Block ID too large");
    }
    // numRsBlocksInGroup2 = 196 % 5 = 1
    int numRsBlocksInGroup2 = numTotalBytes % numRSBlocks;
    // numRsBlocksInGroup1 = 5 - 1 = 4
    int numRsBlocksInGroup1 = numRSBlocks - numRsBlocksInGroup2;
    // numTotalBytesInGroup1 = 196 / 5 = 39
    int numTotalBytesInGroup1 = numTotalBytes / numRSBlocks;
    // numTotalBytesInGroup2 = 39 + 1 = 40
    int numTotalBytesInGroup2 = numTotalBytesInGroup1 + 1;
    // numDataBytesInGroup1 = 66 / 5 = 13
    int numDataBytesInGroup1 = numDataBytes / numRSBlocks;
    // numDataBytesInGroup2 = 13 + 1 = 14
    int numDataBytesInGroup2 = numDataBytesInGroup1 + 1;
    // numEcBytesInGroup1 = 39 - 13 = 26
    int numEcBytesInGroup1 = numTotalBytesInGroup1 - numDataBytesInGroup1;
    // numEcBytesInGroup2 = 40 - 14 = 26
    int numEcBytesInGroup2 = numTotalBytesInGroup2 - numDataBytesInGroup2;
    // Sanity checks.
    // 26 = 26
    if (numEcBytesInGroup1 != numEcBytesInGroup2) {
      throw new DKG_WriterException("EC bytes mismatch");
    }
    // 5 = 4 + 1.
    if (numRSBlocks != numRsBlocksInGroup1 + numRsBlocksInGroup2) {
      throw new DKG_WriterException("RS blocks mismatch");
    }
    // 196 = (13 + 26) * 4 + (14 + 26) * 1
    if (numTotalBytes !=
        ((numDataBytesInGroup1 + numEcBytesInGroup1) *
            numRsBlocksInGroup1) +
            ((numDataBytesInGroup2 + numEcBytesInGroup2) *
                numRsBlocksInGroup2)) {
      throw new DKG_WriterException("Total bytes mismatch");
    }

    if (blockID < numRsBlocksInGroup1) {
      numDataBytesInBlock[0] = numDataBytesInGroup1;
      numECBytesInBlock[0] = numEcBytesInGroup1;
    } else {
      numDataBytesInBlock[0] = numDataBytesInGroup2;
      numECBytesInBlock[0] = numEcBytesInGroup2;
    }
  }


  static DKG_BitArray interleaveWithECBytes(DKG_BitArray bits,
                                            int numTotalBytes,
                                            int numDataBytes,
                                            int numRSBlocks) throws DKG_WriterException {

    // "bits" must have "getNumDataBytes" bytes of data.
    if (bits.getSizeInBytes() != numDataBytes) {
      throw new DKG_WriterException("Number of bits and data bytes does not match");
    }

    // Step 1.  Divide data bytes into blocks and generate error correction bytes for them. We'll
    // store the divided data bytes blocks and error correction bytes blocks into "blocks".
    int dataBytesOffset = 0;
    int maxNumDataBytes = 0;
    int maxNumEcBytes = 0;

    // Since, we know the number of reedsolmon blocks, we can initialize the vector with the number.
    Collection<DKG_BlockPair> blocks = new ArrayList<>(numRSBlocks);

    for (int i = 0; i < numRSBlocks; ++i) {
      int[] numDataBytesInBlock = new int[1];
      int[] numEcBytesInBlock = new int[1];
      getNumDataBytesAndNumECBytesForBlockID(
          numTotalBytes, numDataBytes, numRSBlocks, i,
          numDataBytesInBlock, numEcBytesInBlock);

      int size = numDataBytesInBlock[0];
      byte[] dataBytes = new byte[size];
      bits.toBytes(8 * dataBytesOffset, dataBytes, 0, size);
      byte[] ecBytes = generateECBytes(dataBytes, numEcBytesInBlock[0]);
      blocks.add(new DKG_BlockPair(dataBytes, ecBytes));

      maxNumDataBytes = Math.max(maxNumDataBytes, size);
      maxNumEcBytes = Math.max(maxNumEcBytes, ecBytes.length);
      dataBytesOffset += numDataBytesInBlock[0];
    }
    if (numDataBytes != dataBytesOffset) {
      throw new DKG_WriterException("Data bytes does not match offset");
    }

    DKG_BitArray result = new DKG_BitArray();

    // First, place data blocks.
    for (int i = 0; i < maxNumDataBytes; ++i) {
      for (DKG_BlockPair block : blocks) {
        byte[] dataBytes = block.getDataBytes();
        if (i < dataBytes.length) {
          result.appendBits(dataBytes[i], 8);
        }
      }
    }
    // Then, place error correction blocks.
    for (int i = 0; i < maxNumEcBytes; ++i) {
      for (DKG_BlockPair block : blocks) {
        byte[] ecBytes = block.getErrorCorrectionBytes();
        if (i < ecBytes.length) {
          result.appendBits(ecBytes[i], 8);
        }
      }
    }
    if (numTotalBytes != result.getSizeInBytes()) {  // Should be same.
      throw new DKG_WriterException("Interleaving error: " + numTotalBytes + " and " +
          result.getSizeInBytes() + " differ.");
    }

    return result;
  }

  static byte[] generateECBytes(byte[] dataBytes, int numEcBytesInBlock) {
    int numDataBytes = dataBytes.length;
    int[] toEncode = new int[numDataBytes + numEcBytesInBlock];
    for (int i = 0; i < numDataBytes; i++) {
      toEncode[i] = dataBytes[i] & 0xFF;
    }
    new DKG_ReedSolomonEncoder(DKG_GenericGF.QR_CODE_FIELD_256).encode(toEncode, numEcBytesInBlock);

    byte[] ecBytes = new byte[numEcBytesInBlock];
    for (int i = 0; i < numEcBytesInBlock; i++) {
      ecBytes[i] = (byte) toEncode[numDataBytes + i];
    }
    return ecBytes;
  }


  static void appendModeInfo(DKG_QR_Mode QRMode, DKG_BitArray bits) {
    bits.appendBits(QRMode.getBits(), 4);
  }


  static void appendLengthInfo(int numLetters, DKG_Version DKGVersion, DKG_QR_Mode QRMode, DKG_BitArray bits) throws DKG_WriterException {
    int numBits = QRMode.getCharacterCountInBits(DKGVersion);
    if (numLetters >= (1 << numBits)) {
      throw new DKG_WriterException(numLetters + " is bigger than " + ((1 << numBits) - 1));
    }
    bits.appendBits(numLetters, numBits);
  }

  static void appendBytes(String content,
                          DKG_QR_Mode QRMode,
                          DKG_BitArray bits,
                          Charset encoding) throws DKG_WriterException {
    switch (QRMode) {
      case NUMERIC:
        appendNumericBytes(content, bits);
        break;
      case ALPHANUMERIC:
        appendAlphanumericBytes(content, bits);
        break;
      case BYTE:
        append8BitBytes(content, bits, encoding);
        break;
      case KANJI:
        appendKanjiBytes(content, bits);
        break;
      default:
        throw new DKG_WriterException("Invalid mode: " + QRMode);
    }
  }

  static void appendNumericBytes(CharSequence content, DKG_BitArray bits) {
    int length = content.length();
    int i = 0;
    while (i < length) {
      int num1 = content.charAt(i) - '0';
      if (i + 2 < length) {
        // Encode three numeric letters in ten bits.
        int num2 = content.charAt(i + 1) - '0';
        int num3 = content.charAt(i + 2) - '0';
        bits.appendBits(num1 * 100 + num2 * 10 + num3, 10);
        i += 3;
      } else if (i + 1 < length) {
        // Encode two numeric letters in seven bits.
        int num2 = content.charAt(i + 1) - '0';
        bits.appendBits(num1 * 10 + num2, 7);
        i += 2;
      } else {
        // Encode one numeric letter in four bits.
        bits.appendBits(num1, 4);
        i++;
      }
    }
  }

  static void appendAlphanumericBytes(CharSequence content, DKG_BitArray bits) throws DKG_WriterException {
    int length = content.length();
    int i = 0;
    while (i < length) {
      int code1 = getAlphanumericCode(content.charAt(i));
      if (code1 == -1) {
        throw new DKG_WriterException();
      }
      if (i + 1 < length) {
        int code2 = getAlphanumericCode(content.charAt(i + 1));
        if (code2 == -1) {
          throw new DKG_WriterException();
        }
        // Encode two alphanumeric letters in 11 bits.
        bits.appendBits(code1 * 45 + code2, 11);
        i += 2;
      } else {
        // Encode one alphanumeric letter in six bits.
        bits.appendBits(code1, 6);
        i++;
      }
    }
  }

  static void append8BitBytes(String content, DKG_BitArray bits, Charset encoding) {
    byte[] bytes = content.getBytes(encoding);
    for (byte b : bytes) {
      bits.appendBits(b, 8);
    }
  }

  static void appendKanjiBytes(String content, DKG_BitArray bits) throws DKG_WriterException {
    byte[] bytes = content.getBytes(DKG_StringUtils.SHIFT_JIS_CHARSET);
    if (bytes.length % 2 != 0) {
      throw new DKG_WriterException("Kanji byte size not even");
    }
    int maxI = bytes.length - 1; // bytes.length must be even
    for (int i = 0; i < maxI; i += 2) {
      int byte1 = bytes[i] & 0xFF;
      int byte2 = bytes[i + 1] & 0xFF;
      int code = (byte1 << 8) | byte2;
      int subtracted = -1;
      if (code >= 0x8140 && code <= 0x9ffc) {
        subtracted = code - 0x8140;
      } else if (code >= 0xe040 && code <= 0xebbf) {
        subtracted = code - 0xc140;
      }
      if (subtracted == -1) {
        throw new DKG_WriterException("Invalid byte sequence");
      }
      int encoded = ((subtracted >> 8) * 0xc0) + (subtracted & 0xff);
      bits.appendBits(encoded, 13);
    }
  }

  private static void appendECI(DKG_CharacterSetECI eci, DKG_BitArray bits) {
    bits.appendBits(DKG_QR_Mode.ECI.getBits(), 4);
    // This is correct for values up to 127, which is all we need now.
    bits.appendBits(eci.getValue(), 8);
  }

}
