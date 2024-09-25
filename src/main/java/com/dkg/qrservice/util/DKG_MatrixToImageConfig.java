package com.dkg.qrservice.util;

import java.awt.image.BufferedImage;

public final class DKG_MatrixToImageConfig {

  public static final int BLACK = 0xFF000000;
  public static final int WHITE = 0xFFFFFFFF;
  
  private final int onColor;
  private final int offColor;


  public DKG_MatrixToImageConfig() {
    this(BLACK, WHITE);
  }


  public DKG_MatrixToImageConfig(int onColor, int offColor) {
    this.onColor = onColor;
    this.offColor = offColor;
  }

  public int getPixelOnColor() {
    return onColor;
  }

  public int getPixelOffColor() {
    return offColor;
  }

  int getBufferedImageColorModel() {
    if (onColor == BLACK && offColor == WHITE) {
      // Use faster BINARY if colors match default
      return BufferedImage.TYPE_BYTE_BINARY;
    }
    if (hasTransparency(onColor) || hasTransparency(offColor)) {
      // Use ARGB representation if colors specify non-opaque alpha
      return BufferedImage.TYPE_INT_ARGB;
    }
    // Default otherwise to RGB representation with ignored alpha channel
    return BufferedImage.TYPE_INT_RGB;
  }

  private static boolean hasTransparency(int argb) {
    return (argb & 0xFF000000) != 0xFF000000;
  }

}
