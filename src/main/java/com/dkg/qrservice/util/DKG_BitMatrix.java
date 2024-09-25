package com.dkg.qrservice.util;

import java.util.Arrays;


public final class DKG_BitMatrix implements Cloneable {

  private int width;
  private int height;
  private int rowSize;
  private int[] bits;


  public DKG_BitMatrix(int dimension) {
    this(dimension, dimension);
  }

  public DKG_BitMatrix(int width, int height) {
    if (width < 1 || height < 1) {
      throw new IllegalArgumentException("Both dimensions must be greater than 0");
    }
    this.width = width;
    this.height = height;
    this.rowSize = (width + 31) / 32;
    bits = new int[rowSize * height];
  }

  private DKG_BitMatrix(int width, int height, int rowSize, int[] bits) {
    this.width = width;
    this.height = height;
    this.rowSize = rowSize;
    this.bits = bits;
  }


  public static DKG_BitMatrix parse(boolean[][] image) {
    int height = image.length;
    int width = image[0].length;
    DKG_BitMatrix bits = new DKG_BitMatrix(width, height);
    for (int i = 0; i < height; i++) {
      boolean[] imageI = image[i];
      for (int j = 0; j < width; j++) {
        if (imageI[j]) {
          bits.set(j, i);
        }
      }
    }
    return bits;
  }

  public static DKG_BitMatrix parse(String stringRepresentation, String setString, String unsetString) {
    if (stringRepresentation == null) {
      throw new IllegalArgumentException();
    }

    boolean[] bits = new boolean[stringRepresentation.length()];
    int bitsPos = 0;
    int rowStartPos = 0;
    int rowLength = -1;
    int nRows = 0;
    int pos = 0;
    while (pos < stringRepresentation.length()) {
      if (stringRepresentation.charAt(pos) == '\n' ||
          stringRepresentation.charAt(pos) == '\r') {
        if (bitsPos > rowStartPos) {
          if (rowLength == -1) {
            rowLength = bitsPos - rowStartPos;
          } else if (bitsPos - rowStartPos != rowLength) {
            throw new IllegalArgumentException("row lengths do not match");
          }
          rowStartPos = bitsPos;
          nRows++;
        }
        pos++;
      }  else if (stringRepresentation.startsWith(setString, pos)) {
        pos += setString.length();
        bits[bitsPos] = true;
        bitsPos++;
      } else if (stringRepresentation.startsWith(unsetString, pos)) {
        pos += unsetString.length();
        bits[bitsPos] = false;
        bitsPos++;
      } else {
        throw new IllegalArgumentException(
            "illegal character encountered: " + stringRepresentation.substring(pos));
      }
    }

    // no EOL at end?
    if (bitsPos > rowStartPos) {
      if (rowLength == -1) {
        rowLength = bitsPos - rowStartPos;
      } else if (bitsPos - rowStartPos != rowLength) {
        throw new IllegalArgumentException("row lengths do not match");
      }
      nRows++;
    }

    DKG_BitMatrix matrix = new DKG_BitMatrix(rowLength, nRows);
    for (int i = 0; i < bitsPos; i++) {
      if (bits[i]) {
        matrix.set(i % rowLength, i / rowLength);
      }
    }
    return matrix;
  }


  public boolean get(int x, int y) {
    int offset = y * rowSize + (x / 32);
    return ((bits[offset] >>> (x & 0x1f)) & 1) != 0;
  }


  public void set(int x, int y) {
    int offset = y * rowSize + (x / 32);
    bits[offset] |= 1 << (x & 0x1f);
  }

  public void unset(int x, int y) {
    int offset = y * rowSize + (x / 32);
    bits[offset] &= ~(1 << (x & 0x1f));
  }

  public void flip(int x, int y) {
    int offset = y * rowSize + (x / 32);
    bits[offset] ^= 1 << (x & 0x1f);
  }


  public void flip() {
    int max = bits.length;
    for (int i = 0; i < max; i++) {
      bits[i] = ~bits[i];
    }
  }


  public void xor(DKG_BitMatrix mask) {
    if (width != mask.width || height != mask.height || rowSize != mask.rowSize) {
      throw new IllegalArgumentException("input matrix dimensions do not match");
    }
    DKG_BitArray rowArray = new DKG_BitArray(width);
    for (int y = 0; y < height; y++) {
      int offset = y * rowSize;
      int[] row = mask.getRow(y, rowArray).getBitArray();
      for (int x = 0; x < rowSize; x++) {
        bits[offset + x] ^= row[x];
      }
    }
  }


  public void clear() {
    int max = bits.length;
    for (int i = 0; i < max; i++) {
      bits[i] = 0;
    }
  }


  public void setRegion(int left, int top, int width, int height) {
    if (top < 0 || left < 0) {
      throw new IllegalArgumentException("Left and top must be nonnegative");
    }
    if (height < 1 || width < 1) {
      throw new IllegalArgumentException("Height and width must be at least 1");
    }
    int right = left + width;
    int bottom = top + height;
    if (bottom > this.height || right > this.width) {
      throw new IllegalArgumentException("The region must fit inside the matrix");
    }
    for (int y = top; y < bottom; y++) {
      int offset = y * rowSize;
      for (int x = left; x < right; x++) {
        bits[offset + (x / 32)] |= 1 << (x & 0x1f);
      }
    }
  }


  public DKG_BitArray getRow(int y, DKG_BitArray row) {
    if (row == null || row.getSize() < width) {
      row = new DKG_BitArray(width);
    } else {
      row.clear();
    }
    int offset = y * rowSize;
    for (int x = 0; x < rowSize; x++) {
      row.setBulk(x * 32, bits[offset + x]);
    }
    return row;
  }


  public void setRow(int y, DKG_BitArray row) {
    System.arraycopy(row.getBitArray(), 0, bits, y * rowSize, rowSize);
  }


  public void rotate(int degrees) {
    switch (degrees % 360) {
      case 0:
        return;
      case 90:
        rotate90();
        return;
      case 180:
        rotate180();
        return;
      case 270:
        rotate90();
        rotate180();
        return;
    }
    throw new IllegalArgumentException("degrees must be a multiple of 0, 90, 180, or 270");
  }


  public void rotate180() {
    DKG_BitArray topRow = new DKG_BitArray(width);
    DKG_BitArray bottomRow = new DKG_BitArray(width);
    int maxHeight = (height + 1) / 2;
    for (int i = 0; i < maxHeight; i++) {
      topRow = getRow(i, topRow);
      int bottomRowIndex = height - 1 - i;
      bottomRow = getRow(bottomRowIndex, bottomRow);
      topRow.reverse();
      bottomRow.reverse();
      setRow(i, bottomRow);
      setRow(bottomRowIndex, topRow);
    }
  }


  public void rotate90() {
    int newWidth = height;
    int newHeight = width;
    int newRowSize = (newWidth + 31) / 32;
    int[] newBits = new int[newRowSize * newHeight];

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int offset = y * rowSize + (x / 32);
        if (((bits[offset] >>> (x & 0x1f)) & 1) != 0) {
          int newOffset = (newHeight - 1 - x) * newRowSize + (y / 32);
          newBits[newOffset] |= 1 << (y & 0x1f);
        }
      }
    }
    width = newWidth;
    height = newHeight;
    rowSize = newRowSize;
    bits = newBits;
  }


  public int[] getEnclosingRectangle() {
    int left = width;
    int top = height;
    int right = -1;
    int bottom = -1;

    for (int y = 0; y < height; y++) {
      for (int x32 = 0; x32 < rowSize; x32++) {
        int theBits = bits[y * rowSize + x32];
        if (theBits != 0) {
          if (y < top) {
            top = y;
          }
          if (y > bottom) {
            bottom = y;
          }
          if (x32 * 32 < left) {
            int bit = 0;
            while ((theBits << (31 - bit)) == 0) {
              bit++;
            }
            if ((x32 * 32 + bit) < left) {
              left = x32 * 32 + bit;
            }
          }
          if (x32 * 32 + 31 > right) {
            int bit = 31;
            while ((theBits >>> bit) == 0) {
              bit--;
            }
            if ((x32 * 32 + bit) > right) {
              right = x32 * 32 + bit;
            }
          }
        }
      }
    }

    if (right < left || bottom < top) {
      return null;
    }

    return new int[] {left, top, right - left + 1, bottom - top + 1};
  }


  public int[] getTopLeftOnBit() {
    int bitsOffset = 0;
    while (bitsOffset < bits.length && bits[bitsOffset] == 0) {
      bitsOffset++;
    }
    if (bitsOffset == bits.length) {
      return null;
    }
    int y = bitsOffset / rowSize;
    int x = (bitsOffset % rowSize) * 32;

    int theBits = bits[bitsOffset];
    int bit = 0;
    while ((theBits << (31 - bit)) == 0) {
      bit++;
    }
    x += bit;
    return new int[] {x, y};
  }

  public int[] getBottomRightOnBit() {
    int bitsOffset = bits.length - 1;
    while (bitsOffset >= 0 && bits[bitsOffset] == 0) {
      bitsOffset--;
    }
    if (bitsOffset < 0) {
      return null;
    }

    int y = bitsOffset / rowSize;
    int x = (bitsOffset % rowSize) * 32;

    int theBits = bits[bitsOffset];
    int bit = 31;
    while ((theBits >>> bit) == 0) {
      bit--;
    }
    x += bit;

    return new int[] {x, y};
  }


  public int getWidth() {
    return width;
  }


  public int getHeight() {
    return height;
  }


  public int getRowSize() {
    return rowSize;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof DKG_BitMatrix)) {
      return false;
    }
    DKG_BitMatrix other = (DKG_BitMatrix) o;
    return width == other.width && height == other.height && rowSize == other.rowSize &&
    Arrays.equals(bits, other.bits);
  }

  @Override
  public int hashCode() {
    int hash = width;
    hash = 31 * hash + width;
    hash = 31 * hash + height;
    hash = 31 * hash + rowSize;
    hash = 31 * hash + Arrays.hashCode(bits);
    return hash;
  }


  @Override
  public String toString() {
    return toString("X ", "  ");
  }


  public String toString(String setString, String unsetString) {
    return buildToString(setString, unsetString, "\n");
  }


  @Deprecated
  public String toString(String setString, String unsetString, String lineSeparator) {
    return buildToString(setString, unsetString, lineSeparator);
  }

  private String buildToString(String setString, String unsetString, String lineSeparator) {
    StringBuilder result = new StringBuilder(height * (width + 1));
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        result.append(get(x, y) ? setString : unsetString);
      }
      result.append(lineSeparator);
    }
    return result.toString();
  }

  @Override
  public DKG_BitMatrix clone() {
    return new DKG_BitMatrix(width, height, rowSize, bits.clone());
  }

}
