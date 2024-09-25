package com.dkg.qrservice.util;
public final class DKG_QRCode {

  public static final int NUM_MASK_PATTERNS = 8;

  private DKG_QR_Mode QRMode;
  private DKG_ErrorCorrectionLevel ecLevel;
  private DKG_Version DKGVersion;
  private int maskPattern;
  private DKG_ByteMatrix matrix;

  public DKG_QRCode() {
    maskPattern = -1;
  }


  public DKG_QR_Mode getMode() {
    return QRMode;
  }

  public DKG_ErrorCorrectionLevel getECLevel() {
    return ecLevel;
  }

  public DKG_Version getVersion() {
    return DKGVersion;
  }

  public int getMaskPattern() {
    return maskPattern;
  }

  public DKG_ByteMatrix getMatrix() {
    return matrix;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(200);
    result.append("<<\n");
    result.append(" mode: ");
    result.append(QRMode);
    result.append("\n ecLevel: ");
    result.append(ecLevel);
    result.append("\n version: ");
    result.append(DKGVersion);
    result.append("\n maskPattern: ");
    result.append(maskPattern);
    if (matrix == null) {
      result.append("\n matrix: null\n");
    } else {
      result.append("\n matrix:\n");
      result.append(matrix);
    }
    result.append(">>\n");
    return result.toString();
  }

  public void setMode(DKG_QR_Mode value) {
    QRMode = value;
  }

  public void setECLevel(DKG_ErrorCorrectionLevel value) {
    ecLevel = value;
  }

  public void setVersion(DKG_Version DKGVersion) {
    this.DKGVersion = DKGVersion;
  }

  public void setMaskPattern(int value) {
    maskPattern = value;
  }

  public void setMatrix(DKG_ByteMatrix value) {
    matrix = value;
  }

  // Check if "mask_pattern" is valid.
  public static boolean isValidMaskPattern(int maskPattern) {
    return maskPattern >= 0 && maskPattern < NUM_MASK_PATTERNS;
  }

}
