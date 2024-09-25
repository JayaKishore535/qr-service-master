package com.dkg.qrservice.util;

import java.util.Map;


public final class DKG_QRCodeDKGWriter implements DKG_Writer {

  private static final int QUIET_ZONE_SIZE = 4;

  @Override
  public DKG_BitMatrix encode(String contents, DKG_BarcodeFormat format, int width, int height)
      throws DKG_WriterException {

    return encode(contents, format, width, height, null);
  }

  @Override
  public DKG_BitMatrix encode(String contents,
                              DKG_BarcodeFormat format,
                              int width,
                              int height,
                              Map<DKG_EncodeHintType,?> hints) throws DKG_WriterException {

    if (contents.isEmpty()) {
      throw new IllegalArgumentException("Found empty contents");
    }

    if (format != DKG_BarcodeFormat.QR_CODE) {
      throw new IllegalArgumentException("Can only encode QR_CODE, but got " + format);
    }

    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Requested dimensions are too small: " + width + 'x' +
          height);
    }

    DKG_ErrorCorrectionLevel DKGErrorCorrectionLevel = DKG_ErrorCorrectionLevel.L;
    int quietZone = QUIET_ZONE_SIZE;
    if (hints != null) {
      if (hints.containsKey(DKG_EncodeHintType.ERROR_CORRECTION)) {
        DKGErrorCorrectionLevel = DKG_ErrorCorrectionLevel.valueOf(hints.get(DKG_EncodeHintType.ERROR_CORRECTION).toString());
      }
      if (hints.containsKey(DKG_EncodeHintType.MARGIN)) {
        quietZone = Integer.parseInt(hints.get(DKG_EncodeHintType.MARGIN).toString());
      }
    }

    DKG_QRCode code = DKG_Encoder.encode(contents, DKGErrorCorrectionLevel, hints);
    return renderResult(code, width, height, quietZone);
  }

  private static DKG_BitMatrix renderResult(DKG_QRCode code, int width, int height, int quietZone) {
    DKG_ByteMatrix input = code.getMatrix();
    if (input == null) {
      throw new IllegalStateException();
    }
    int inputWidth = input.getWidth();
    int inputHeight = input.getHeight();
    int qrWidth = inputWidth + (quietZone * 2);
    int qrHeight = inputHeight + (quietZone * 2);
    int outputWidth = Math.max(width, qrWidth);
    int outputHeight = Math.max(height, qrHeight);

    int multiple = Math.min(outputWidth / qrWidth, outputHeight / qrHeight);
    // Padding includes both the quiet zone and the extra white pixels to accommodate the requested
    // dimensions. For example, if input is 25x25 the QR will be 33x33 including the quiet zone.
    // If the requested size is 200x160, the multiple will be 4, for a QR of 132x132. These will
    // handle all the padding from 100x100 (the actual QR) up to 200x160.
    int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
    int topPadding = (outputHeight - (inputHeight * multiple)) / 2;

    DKG_BitMatrix output = new DKG_BitMatrix(outputWidth, outputHeight);

    for (int inputY = 0, outputY = topPadding; inputY < inputHeight; inputY++, outputY += multiple) {
      // Write the contents of this row of the barcode
      for (int inputX = 0, outputX = leftPadding; inputX < inputWidth; inputX++, outputX += multiple) {
        if (input.get(inputX, inputY) == 1) {
          output.setRegion(outputX, outputY, multiple, multiple);
        }
      }
    }

    return output;
  }

}
