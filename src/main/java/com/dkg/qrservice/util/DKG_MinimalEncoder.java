package com.dkg.qrservice.util;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

final class DKG_MinimalEncoder {

  private enum VersionSize {
    SMALL("version 1-9"),
    MEDIUM("version 10-26"),
    LARGE("version 27-40");

    private final String description;

    VersionSize(String description) {
      this.description = description;
    }

    public String toString() {
      return description;
    }
  }

  private final String stringToEncode;
  private final boolean isGS1;
  private final DKG_ECIEncoderSet encoders;
  private final DKG_ErrorCorrectionLevel ecLevel;


  DKG_MinimalEncoder(String stringToEncode, Charset priorityCharset, boolean isGS1, DKG_ErrorCorrectionLevel ecLevel) {
    this.stringToEncode = stringToEncode;
    this.isGS1 = isGS1;
    this.encoders = new DKG_ECIEncoderSet(stringToEncode, priorityCharset, -1);
    this.ecLevel = ecLevel;
  }


  static ResultList encode(String stringToEncode, DKG_Version DKGVersion, Charset priorityCharset, boolean isGS1,
                           DKG_ErrorCorrectionLevel ecLevel) throws DKG_WriterException {
    return new DKG_MinimalEncoder(stringToEncode, priorityCharset, isGS1, ecLevel).encode(DKGVersion);
  }

  ResultList encode(DKG_Version DKGVersion) throws DKG_WriterException {
    if (DKGVersion == null) { // compute minimal encoding trying the three version sizes.
      DKG_Version[] DKGVersions = { getVersion(VersionSize.SMALL),
                             getVersion(VersionSize.MEDIUM),
                             getVersion(VersionSize.LARGE) };
      ResultList[] results = { encodeSpecificVersion(DKGVersions[0]),
                               encodeSpecificVersion(DKGVersions[1]),
                               encodeSpecificVersion(DKGVersions[2]) };
      int smallestSize = Integer.MAX_VALUE;
      int smallestResult = -1;
      for (int i = 0; i < 3; i++) {
        int size = results[i].getSize();
        if (DKG_Encoder.willFit(size, DKGVersions[i], ecLevel) && size < smallestSize) {
          smallestSize = size;
          smallestResult = i;
        }
      }
      if (smallestResult < 0) {
        throw new DKG_WriterException("Data too big for any version");
      }
      return results[smallestResult];
    } else { // compute minimal encoding for a given version
      ResultList result = encodeSpecificVersion(DKGVersion);
      if (!DKG_Encoder.willFit(result.getSize(), getVersion(getVersionSize(result.getVersion())), ecLevel)) {
        throw new DKG_WriterException("Data too big for version" + DKGVersion);
      }
      return result;
    }
  }

  static VersionSize getVersionSize(DKG_Version DKGVersion) {
    return DKGVersion.getVersionNumber() <= 9 ? VersionSize.SMALL : DKGVersion.getVersionNumber() <= 26 ?
      VersionSize.MEDIUM : VersionSize.LARGE;
  }

  static DKG_Version getVersion(VersionSize versionSize) {
    switch (versionSize) {
      case SMALL:
        return com.dkg.qrservice.util.DKG_Version.getVersionForNumber(9);
      case MEDIUM:
        return com.dkg.qrservice.util.DKG_Version.getVersionForNumber(26);
      case LARGE:
      default:
        return com.dkg.qrservice.util.DKG_Version.getVersionForNumber(40);
    }
  }

  static boolean isNumeric(char c) {
    return c >= '0' && c <= '9';
  }

  static boolean isDoubleByteKanji(char c) {
    return DKG_Encoder.isOnlyDoubleByteKanji(String.valueOf(c));
  }

  static boolean isAlphanumeric(char c) {
    return DKG_Encoder.getAlphanumericCode(c) != -1;
  }

  boolean canEncode(DKG_QR_Mode QRMode, char c) {
    switch (QRMode) {
      case KANJI: return isDoubleByteKanji(c);
      case ALPHANUMERIC: return isAlphanumeric(c);
      case NUMERIC: return isNumeric(c);
      case BYTE: return true; // any character can be encoded as byte(s). Up to the caller to manage splitting into
                              // multiple bytes when String.getBytes(Charset) return more than one byte.
      default:
        return false;
    }
  }

  static int getCompactedOrdinal(DKG_QR_Mode QRMode) {
    if (QRMode == null) {
      return 0;
    }
    switch (QRMode) {
      case KANJI:
        return 0;
      case ALPHANUMERIC:
        return 1;
      case NUMERIC:
        return 2;
      case BYTE:
        return 3;
      default:
        throw new IllegalStateException("Illegal mode " + QRMode);
    }
  }

  void addEdge(Edge[][][] edges, int position, Edge edge) {
    int vertexIndex = position + edge.characterLength;
    Edge[] modeEdges = edges[vertexIndex][edge.charsetEncoderIndex];
    int modeOrdinal = getCompactedOrdinal(edge.QRMode);
    if (modeEdges[modeOrdinal] == null || modeEdges[modeOrdinal].cachedTotalSize > edge.cachedTotalSize) {
      modeEdges[modeOrdinal] = edge;
    }
  }

  void addEdges(DKG_Version DKGVersion, Edge[][][] edges, int from, Edge previous) {
    int start = 0;
    int end = encoders.length();
    int priorityEncoderIndex = encoders.getPriorityEncoderIndex();
    if (priorityEncoderIndex >= 0 && encoders.canEncode(stringToEncode.charAt(from),priorityEncoderIndex)) {
      start = priorityEncoderIndex;
      end = priorityEncoderIndex + 1;
    }

    for (int i = start; i < end; i++) {
      if (encoders.canEncode(stringToEncode.charAt(from), i)) {
        addEdge(edges, from, new Edge(DKG_QR_Mode.BYTE, from, i, 1, previous, DKGVersion));
      }
    }

    if (canEncode(DKG_QR_Mode.KANJI, stringToEncode.charAt(from))) {
      addEdge(edges, from, new Edge(DKG_QR_Mode.KANJI, from, 0, 1, previous, DKGVersion));
    }

    int inputLength = stringToEncode.length();
    if (canEncode(DKG_QR_Mode.ALPHANUMERIC, stringToEncode.charAt(from))) {
      addEdge(edges, from, new Edge(DKG_QR_Mode.ALPHANUMERIC, from, 0, from + 1 >= inputLength ||
          !canEncode(DKG_QR_Mode.ALPHANUMERIC, stringToEncode.charAt(from + 1)) ? 1 : 2, previous, DKGVersion));
    }

    if (canEncode(DKG_QR_Mode.NUMERIC, stringToEncode.charAt(from))) {
      addEdge(edges, from, new Edge(DKG_QR_Mode.NUMERIC, from, 0, from + 1 >= inputLength ||
          !canEncode(DKG_QR_Mode.NUMERIC, stringToEncode.charAt(from + 1)) ? 1 : from + 2 >= inputLength ||
          !canEncode(DKG_QR_Mode.NUMERIC, stringToEncode.charAt(from + 2)) ? 2 : 3, previous, DKGVersion));
    }
  }
  ResultList encodeSpecificVersion(DKG_Version DKGVersion) throws DKG_WriterException {


    int inputLength = stringToEncode.length();

    Edge[][][] edges = new Edge[inputLength + 1][encoders.length()][4];
    addEdges(DKGVersion, edges, 0, null);

    for (int i = 1; i <= inputLength; i++) {
      for (int j = 0; j < encoders.length(); j++) {
        for (int k = 0; k < 4; k++) {
          if (edges[i][j][k] != null && i < inputLength) {
            addEdges(DKGVersion, edges, i, edges[i][j][k]);
          }
        }
      }

    }
    int minimalJ = -1;
    int minimalK = -1;
    int minimalSize = Integer.MAX_VALUE;
    for (int j = 0; j < encoders.length(); j++) {
      for (int k = 0; k < 4; k++) {
        if (edges[inputLength][j][k] != null) {
          Edge edge = edges[inputLength][j][k];
          if (edge.cachedTotalSize < minimalSize) {
            minimalSize = edge.cachedTotalSize;
            minimalJ = j;
            minimalK = k;
          }
        }
      }
    }
    if (minimalJ < 0) {
      throw new DKG_WriterException("Internal error: failed to encode \"" + stringToEncode + "\"");
    }
    return new ResultList(DKGVersion, edges[inputLength][minimalJ][minimalK]);
  }

  private final class Edge {
    private final DKG_QR_Mode QRMode;
    private final int fromPosition;
    private final int charsetEncoderIndex;
    private final int characterLength;
    private final Edge previous;
    private final int cachedTotalSize;

    private Edge(DKG_QR_Mode QRMode, int fromPosition, int charsetEncoderIndex, int characterLength, Edge previous,
                 DKG_Version DKGVersion) {
      this.QRMode = QRMode;
      this.fromPosition = fromPosition;
      this.charsetEncoderIndex = QRMode == DKG_QR_Mode.BYTE || previous == null ? charsetEncoderIndex :
          previous.charsetEncoderIndex; // inherit the encoding if not of type BYTE
      this.characterLength = characterLength;
      this.previous = previous;

      int size = previous != null ? previous.cachedTotalSize : 0;

      boolean needECI = QRMode == DKG_QR_Mode.BYTE &&
          (previous == null && this.charsetEncoderIndex != 0) || // at the beginning and charset is not ISO-8859-1
          (previous != null && this.charsetEncoderIndex != previous.charsetEncoderIndex);

      if (previous == null || QRMode != previous.QRMode || needECI) {
        size += 4 + QRMode.getCharacterCountInBits(DKGVersion);
      }
      switch (QRMode) {
        case KANJI:
          size += 13;
          break;
        case ALPHANUMERIC:
          size += characterLength == 1 ? 6 : 11;
          break;
        case NUMERIC:
          size += characterLength == 1 ? 4 : characterLength == 2 ? 7 : 10;
          break;
        case BYTE:
          size += 8 * encoders.encode(stringToEncode.substring(fromPosition, fromPosition + characterLength),
              charsetEncoderIndex).length;
          if (needECI) {
            size += 4 + 8; // the ECI assignment numbers for ISO-8859-x, UTF-8 and UTF-16 are all 8 bit long
          }
          break;
      }
      cachedTotalSize = size;
    }
  }

  final class ResultList {

    private final List<ResultList.ResultNode> list = new ArrayList<>();
    private final DKG_Version DKGVersion;

    ResultList(DKG_Version DKGVersion, Edge solution) {
      int length = 0;
      Edge current = solution;
      boolean containsECI = false;

      while (current != null) {
        length += current.characterLength;
        Edge previous = current.previous;

        boolean needECI = current.QRMode == DKG_QR_Mode.BYTE &&
            (previous == null && current.charsetEncoderIndex != 0) || // at the beginning and charset is not ISO-8859-1
            (previous != null && current.charsetEncoderIndex != previous.charsetEncoderIndex);

        if (needECI) {
          containsECI = true;
        }

        if (previous == null || previous.QRMode != current.QRMode || needECI) {
          list.add(0, new ResultNode(current.QRMode, current.fromPosition, current.charsetEncoderIndex, length));
          length = 0;
        }

        if (needECI) {
          list.add(0, new ResultNode(DKG_QR_Mode.ECI, current.fromPosition, current.charsetEncoderIndex, 0));
        }
        current = previous;
      }

      if (isGS1) {
        ResultNode first = list.get(0);
        if (first != null && first.QRMode != DKG_QR_Mode.ECI && containsECI) {
          // prepend a default character set ECI
          list.add(0, new ResultNode(DKG_QR_Mode.ECI, 0, 0, 0));
        }
        first = list.get(0);
        // prepend or insert a FNC1_FIRST_POSITION after the ECI (if any)
        list.add(first.QRMode != DKG_QR_Mode.ECI ? 0 : 1, new ResultNode(DKG_QR_Mode.FNC1_FIRST_POSITION, 0, 0, 0));
      }

      // set version to smallest version into which the bits fit.
      int versionNumber = DKGVersion.getVersionNumber();
      int lowerLimit;
      int upperLimit;
      switch (getVersionSize(DKGVersion)) {
        case SMALL:
          lowerLimit = 1;
          upperLimit = 9;
          break;
        case MEDIUM:
          lowerLimit = 10;
          upperLimit = 26;
          break;
        case LARGE:
        default:
          lowerLimit = 27;
          upperLimit = 40;
          break;
      }
      int size = getSize(DKGVersion);
      // increase version if needed
      while (versionNumber < upperLimit && !DKG_Encoder.willFit(size, com.dkg.qrservice.util.DKG_Version.getVersionForNumber(versionNumber),
        ecLevel)) {
        versionNumber++;
      }
      // shrink version if possible
      while (versionNumber > lowerLimit && DKG_Encoder.willFit(size, com.dkg.qrservice.util.DKG_Version.getVersionForNumber(versionNumber - 1),
        ecLevel)) {
        versionNumber--;
      }
      this.DKGVersion = com.dkg.qrservice.util.DKG_Version.getVersionForNumber(versionNumber);
    }

    /**
     * returns the size in bits
     */
    int getSize() {
      return getSize(DKGVersion);
    }

    private int getSize(DKG_Version DKGVersion) {
      int result = 0;
      for (ResultNode resultNode : list) {
        result += resultNode.getSize(DKGVersion);
      }
      return result;
    }

    /**
     * appends the bits
     */
    void getBits(DKG_BitArray bits) throws DKG_WriterException {
      for (ResultNode resultNode : list) {
        resultNode.getBits(bits);
      }
    }

    DKG_Version getVersion() {
      return DKGVersion;
    }

    public String toString() {
      StringBuilder result = new StringBuilder();
      ResultNode previous = null;
      for (ResultNode current : list) {
        if (previous != null) {
          result.append(",");
        }
        result.append(current.toString());
        previous = current;
      }
      return result.toString();
    }

    final class ResultNode {

      private final DKG_QR_Mode QRMode;
      private final int fromPosition;
      private final int charsetEncoderIndex;
      private final int characterLength;

      ResultNode(DKG_QR_Mode QRMode, int fromPosition, int charsetEncoderIndex, int characterLength) {
        this.QRMode = QRMode;
        this.fromPosition = fromPosition;
        this.charsetEncoderIndex = charsetEncoderIndex;
        this.characterLength = characterLength;
      }

      /**
       * returns the size in bits
       */
      private int getSize(DKG_Version DKGVersion) {
        int size = 4 + QRMode.getCharacterCountInBits(DKGVersion);
        switch (QRMode) {
          case KANJI:
            size += 13 * characterLength;
            break;
          case ALPHANUMERIC:
            size += (characterLength / 2) * 11;
            size += (characterLength % 2) == 1 ? 6 : 0;
            break;
          case NUMERIC:
            size += (characterLength / 3) * 10;
            int rest = characterLength % 3;
            size += rest == 1 ? 4 : rest == 2 ? 7 : 0;
            break;
          case BYTE:
            size += 8 * getCharacterCountIndicator();
            break;
          case ECI:
            size += 8; // the ECI assignment numbers for ISO-8859-x, UTF-8 and UTF-16 are all 8 bit long
        }
        return size;
      }


      private int getCharacterCountIndicator() {
        return QRMode == DKG_QR_Mode.BYTE ?
            encoders.encode(stringToEncode.substring(fromPosition, fromPosition + characterLength),
            charsetEncoderIndex).length : characterLength;
      }

      /**
       * appends the bits
       */
      private void getBits(DKG_BitArray bits) throws DKG_WriterException {
        bits.appendBits(QRMode.getBits(), 4);
        if (characterLength > 0) {
          int length = getCharacterCountIndicator();
          bits.appendBits(length, QRMode.getCharacterCountInBits(DKGVersion));
        }
        if (QRMode == DKG_QR_Mode.ECI) {
          bits.appendBits(encoders.getECIValue(charsetEncoderIndex), 8);
        } else if (characterLength > 0) {
          // append data
          DKG_Encoder.appendBytes(stringToEncode.substring(fromPosition, fromPosition + characterLength), QRMode, bits,
              encoders.getCharset(charsetEncoderIndex));
        }
      }

      public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(QRMode).append('(');
        if (QRMode == DKG_QR_Mode.ECI) {
          result.append(encoders.getCharset(charsetEncoderIndex).displayName());
        } else {
          result.append(makePrintable(stringToEncode.substring(fromPosition, fromPosition + characterLength)));
        }
        result.append(')');
        return result.toString();
      }

      private String makePrintable(String s) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
          if (s.charAt(i) < 32 || s.charAt(i) > 126) {
            result.append('.');
          } else {
            result.append(s.charAt(i));
          }
        }
        return result.toString();
      }
    }
  }
}
