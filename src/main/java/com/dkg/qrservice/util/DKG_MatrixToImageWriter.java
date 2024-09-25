package com.dkg.qrservice.util;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.OutputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.nio.file.Path;


public final class DKG_MatrixToImageWriter {

  private static final DKG_MatrixToImageConfig DEFAULT_CONFIG = new DKG_MatrixToImageConfig();

  private DKG_MatrixToImageWriter() {}


  public static BufferedImage toBufferedImage(DKG_BitMatrix matrix) {
    return toBufferedImage(matrix, DEFAULT_CONFIG);
  }

  public static BufferedImage toBufferedImage(DKG_BitMatrix matrix, DKG_MatrixToImageConfig config) {
    int width = matrix.getWidth();
    int height = matrix.getHeight();
    BufferedImage image = new BufferedImage(width, height, config.getBufferedImageColorModel());
    int onColor = config.getPixelOnColor();
    int offColor = config.getPixelOffColor();
    int[] rowPixels = new int[width];
    DKG_BitArray row = new DKG_BitArray(width);
    for (int y = 0; y < height; y++) {
      row = matrix.getRow(y, row);
      for (int x = 0; x < width; x++) {
        rowPixels[x] = row.get(x) ? onColor : offColor;
      }
      image.setRGB(0, y, width, 1, rowPixels, 0, width);
    }
    return image;
  }


  @Deprecated
  public static void writeToFile(DKG_BitMatrix matrix, String format, File file) throws IOException {
    writeToPath(matrix, format, file.toPath());
  }


  public static void writeToPath(DKG_BitMatrix matrix, String format, Path file) throws IOException {
    writeToPath(matrix, format, file, DEFAULT_CONFIG);
  }


  @Deprecated
  public static void writeToFile(DKG_BitMatrix matrix, String format, File file, DKG_MatrixToImageConfig config)
      throws IOException {
    writeToPath(matrix, format, file.toPath(), config);
  }

  public static void writeToPath(DKG_BitMatrix matrix, String format, Path file, DKG_MatrixToImageConfig config)
      throws IOException {
    BufferedImage image = toBufferedImage(matrix, config);
    if (!ImageIO.write(image, format, file.toFile())) {
      throw new IOException("Could not write an image of format " + format + " to " + file);
    }
  }


  public static void writeToStream(DKG_BitMatrix matrix, String format, OutputStream stream) throws IOException {
    writeToStream(matrix, format, stream, DEFAULT_CONFIG);
  }

  public static void writeToStream(DKG_BitMatrix matrix, String format, OutputStream stream, DKG_MatrixToImageConfig config)
      throws IOException {  
    BufferedImage image = toBufferedImage(matrix, config);
    if (!ImageIO.write(image, format, stream)) {
      throw new IOException("Could not write an image of format " + format);
    }
  }

}