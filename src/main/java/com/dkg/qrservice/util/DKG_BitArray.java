package com.dkg.qrservice.util;

import java.util.Arrays;


public final class DKG_BitArray implements Cloneable {

  private static final int[] EMPTY_BITS = {};
  private static final float LOAD_FACTOR = 0.75f;

  private int[] bits;
  private int size;

  public DKG_BitArray() {
    this.size = 0;
    this.bits = EMPTY_BITS;
  }

  public DKG_BitArray(int size) {
    this.size = size;
    this.bits = makeArray(size);
  }

  // For testing only
  DKG_BitArray(int[] bits, int size) {
    this.bits = bits;
    this.size = size;
  }

  public int getSize() {
    return size;
  }

  public int getSizeInBytes() {
    return (size + 7) / 8;
  }

  private void ensureCapacity(int newSize) {
    if (newSize > bits.length * 32) {
      int[] newBits = makeArray((int) Math.ceil(newSize / LOAD_FACTOR));
      System.arraycopy(bits, 0, newBits, 0, bits.length);
      this.bits = newBits;
    }
  }


  public boolean get(int i) {
    return (bits[i / 32] & (1 << (i & 0x1F))) != 0;
  }


  public void set(int i) {
    bits[i / 32] |= 1 << (i & 0x1F);
  }


  public void flip(int i) {
    bits[i / 32] ^= 1 << (i & 0x1F);
  }


  public int getNextSet(int from) {
    if (from >= size) {
      return size;
    }
    int bitsOffset = from / 32;
    int currentBits = bits[bitsOffset];
    // mask off lesser bits first
    currentBits &= -(1 << (from & 0x1F));
    while (currentBits == 0) {
      if (++bitsOffset == bits.length) {
        return size;
      }
      currentBits = bits[bitsOffset];
    }
    int result = (bitsOffset * 32) + Integer.numberOfTrailingZeros(currentBits);
    return Math.min(result, size);
  }


  public int getNextUnset(int from) {
    if (from >= size) {
      return size;
    }
    int bitsOffset = from / 32;
    int currentBits = ~bits[bitsOffset];
    // mask off lesser bits first
    currentBits &= -(1 << (from & 0x1F));
    while (currentBits == 0) {
      if (++bitsOffset == bits.length) {
        return size;
      }
      currentBits = ~bits[bitsOffset];
    }
    int result = (bitsOffset * 32) + Integer.numberOfTrailingZeros(currentBits);
    return Math.min(result, size);
  }


  public void setBulk(int i, int newBits) {
    bits[i / 32] = newBits;
  }


  public void setRange(int start, int end) {
    if (end < start || start < 0 || end > size) {
      throw new IllegalArgumentException();
    }
    if (end == start) {
      return;
    }
    end--; // will be easier to treat this as the last actually set bit -- inclusive
    int firstInt = start / 32;
    int lastInt = end / 32;
    for (int i = firstInt; i <= lastInt; i++) {
      int firstBit = i > firstInt ? 0 : start & 0x1F;
      int lastBit = i < lastInt ? 31 : end & 0x1F;
      // Ones from firstBit to lastBit, inclusive
      int mask = (2 << lastBit) - (1 << firstBit);
      bits[i] |= mask;
    }
  }


  public void clear() {
    int max = bits.length;
    for (int i = 0; i < max; i++) {
      bits[i] = 0;
    }
  }


  public boolean isRange(int start, int end, boolean value) {
    if (end < start || start < 0 || end > size) {
      throw new IllegalArgumentException();
    }
    if (end == start) {
      return true; // empty range matches
    }
    end--; // will be easier to treat this as the last actually set bit -- inclusive
    int firstInt = start / 32;
    int lastInt = end / 32;
    for (int i = firstInt; i <= lastInt; i++) {
      int firstBit = i > firstInt ? 0 : start & 0x1F;
      int lastBit = i < lastInt ? 31 : end & 0x1F;
      // Ones from firstBit to lastBit, inclusive
      int mask = (2 << lastBit) - (1 << firstBit);

      // Return false if we're looking for 1s and the masked bits[i] isn't all 1s (that is,
      // equals the mask, or we're looking for 0s and the masked portion is not all 0s
      if ((bits[i] & mask) != (value ? mask : 0)) {
        return false;
      }
    }
    return true;
  }

  public void appendBit(boolean bit) {
    ensureCapacity(size + 1);
    if (bit) {
      bits[size / 32] |= 1 << (size & 0x1F);
    }
    size++;
  }


  public void appendBits(int value, int numBits) {
    if (numBits < 0 || numBits > 32) {
      throw new IllegalArgumentException("Num bits must be between 0 and 32");
    }
    int nextSize = size;
    ensureCapacity(nextSize + numBits);
    for (int numBitsLeft = numBits - 1; numBitsLeft >= 0; numBitsLeft--) {
      if ((value & (1 << numBitsLeft)) != 0) {
        bits[nextSize / 32] |= 1 << (nextSize & 0x1F);
      }
      nextSize++;
    }
    size = nextSize;
  }

  public void appendBitArray(DKG_BitArray other) {
    int otherSize = other.size;
    ensureCapacity(size + otherSize);
    for (int i = 0; i < otherSize; i++) {
      appendBit(other.get(i));
    }
  }

  public void xor(DKG_BitArray other) {
    if (size != other.size) {
      throw new IllegalArgumentException("Sizes don't match");
    }
    for (int i = 0; i < bits.length; i++) {
      // The last int could be incomplete (i.e. not have 32 bits in
      // it) but there is no problem since 0 XOR 0 == 0.
      bits[i] ^= other.bits[i];
    }
  }


  public void toBytes(int bitOffset, byte[] array, int offset, int numBytes) {
    for (int i = 0; i < numBytes; i++) {
      int theByte = 0;
      for (int j = 0; j < 8; j++) {
        if (get(bitOffset)) {
          theByte |= 1 << (7 - j);
        }
        bitOffset++;
      }
      array[offset + i] = (byte) theByte;
    }
  }


  public int[] getBitArray() {
    return bits;
  }

  /**
   * Reverses all bits in the array.
   */
  public void reverse() {
    int[] newBits = new int[bits.length];
    // reverse all int's first
    int len = (size - 1) / 32;
    int oldBitsLen = len + 1;
    for (int i = 0; i < oldBitsLen; i++) {
      newBits[len - i] = Integer.reverse(bits[i]);
    }
    // now correct the int's if the bit size isn't a multiple of 32
    if (size != oldBitsLen * 32) {
      int leftOffset = oldBitsLen * 32 - size;
      int currentInt = newBits[0] >>> leftOffset;
      for (int i = 1; i < oldBitsLen; i++) {
        int nextInt = newBits[i];
        currentInt |= nextInt << (32 - leftOffset);
        newBits[i - 1] = currentInt;
        currentInt = nextInt >>> leftOffset;
      }
      newBits[oldBitsLen - 1] = currentInt;
    }
    bits = newBits;
  }

  private static int[] makeArray(int size) {
    return new int[(size + 31) / 32];
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof DKG_BitArray)) {
      return false;
    }
    DKG_BitArray other = (DKG_BitArray) o;
    return size == other.size && Arrays.equals(bits, other.bits);
  }

  @Override
  public int hashCode() {
    return 31 * size + Arrays.hashCode(bits);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(size + (size / 8) + 1);
    for (int i = 0; i < size; i++) {
      if ((i & 0x07) == 0) {
        result.append(' ');
      }
      result.append(get(i) ? 'X' : '.');
    }
    return result.toString();
  }

  @Override
  public DKG_BitArray clone() {
    return new DKG_BitArray(bits.clone(), size);
  }

}
