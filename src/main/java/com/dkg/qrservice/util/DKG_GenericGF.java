package com.dkg.qrservice.util;

public final class DKG_GenericGF {

  public static final DKG_GenericGF AZTEC_DATA_12 = new DKG_GenericGF(0b1000001101001, 4096, 1); // x^12 + x^6 + x^5 + x^3 + 1
  public static final DKG_GenericGF AZTEC_DATA_10 = new DKG_GenericGF(0b10000001001, 1024, 1); // x^10 + x^3 + 1
  public static final DKG_GenericGF AZTEC_DATA_6 = new DKG_GenericGF(0b1000011, 64, 1); // x^6 + x + 1
  public static final DKG_GenericGF AZTEC_PARAM = new DKG_GenericGF(0b10011, 16, 1); // x^4 + x + 1
  public static final DKG_GenericGF QR_CODE_FIELD_256 = new DKG_GenericGF(0b100011101, 256, 0); // x^8 + x^4 + x^3 + x^2 + 1
  public static final DKG_GenericGF DATA_MATRIX_FIELD_256 = new DKG_GenericGF(0b100101101, 256, 1); // x^8 + x^5 + x^3 + x^2 + 1
  public static final DKG_GenericGF AZTEC_DATA_8 = DATA_MATRIX_FIELD_256;
  public static final DKG_GenericGF MAXICODE_FIELD_64 = AZTEC_DATA_6;

  private final int[] expTable;
  private final int[] logTable;
  private final DKG_GenericGFPoly zero;
  private final DKG_GenericGFPoly one;
  private final int size;
  private final int primitive;
  private final int generatorBase;


  public DKG_GenericGF(int primitive, int size, int b) {
    this.primitive = primitive;
    this.size = size;
    this.generatorBase = b;

    expTable = new int[size];
    logTable = new int[size];
    int x = 1;
    for (int i = 0; i < size; i++) {
      expTable[i] = x;
      x *= 2; // 2 (the polynomial x) is a primitive element
      if (x >= size) {
        x ^= primitive;
        x &= size - 1;
      }
    }
    for (int i = 0; i < size - 1; i++) {
      logTable[expTable[i]] = i;
    }
    // logTable[0] == 0 but this should never be used
    zero = new DKG_GenericGFPoly(this, new int[]{0});
    one = new DKG_GenericGFPoly(this, new int[]{1});
  }

  DKG_GenericGFPoly getZero() {
    return zero;
  }

  DKG_GenericGFPoly getOne() {
    return one;
  }


  DKG_GenericGFPoly buildMonomial(int degree, int coefficient) {
    if (degree < 0) {
      throw new IllegalArgumentException();
    }
    if (coefficient == 0) {
      return zero;
    }
    int[] coefficients = new int[degree + 1];
    coefficients[0] = coefficient;
    return new DKG_GenericGFPoly(this, coefficients);
  }

  static int addOrSubtract(int a, int b) {
    return a ^ b;
  }


  int exp(int a) {
    return expTable[a];
  }

  int log(int a) {
    if (a == 0) {
      throw new IllegalArgumentException();
    }
    return logTable[a];
  }


  int inverse(int a) {
    if (a == 0) {
      throw new ArithmeticException();
    }
    return expTable[size - logTable[a] - 1];
  }


  int multiply(int a, int b) {
    if (a == 0 || b == 0) {
      return 0;
    }
    return expTable[(logTable[a] + logTable[b]) % (size - 1)];
  }

  public int getSize() {
    return size;
  }

  public int getGeneratorBase() {
    return generatorBase;
  }

  @Override
  public String toString() {
    return "GF(0x" + Integer.toHexString(primitive) + ',' + size + ')';
  }

}
