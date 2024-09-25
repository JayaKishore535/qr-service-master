package com.dkg.qrservice.util;

import java.util.ArrayList;
import java.util.List;


public final class DKG_ReedSolomonEncoder {

  private final DKG_GenericGF field;
  private final List<DKG_GenericGFPoly> cachedGenerators;

  public DKG_ReedSolomonEncoder(DKG_GenericGF field) {
    this.field = field;
    this.cachedGenerators = new ArrayList<>();
    cachedGenerators.add(new DKG_GenericGFPoly(field, new int[]{1}));
  }

  private DKG_GenericGFPoly buildGenerator(int degree) {
    if (degree >= cachedGenerators.size()) {
      DKG_GenericGFPoly lastGenerator = cachedGenerators.get(cachedGenerators.size() - 1);
      for (int d = cachedGenerators.size(); d <= degree; d++) {
        DKG_GenericGFPoly nextGenerator = lastGenerator.multiply(
            new DKG_GenericGFPoly(field, new int[] { 1, field.exp(d - 1 + field.getGeneratorBase()) }));
        cachedGenerators.add(nextGenerator);
        lastGenerator = nextGenerator;
      }
    }
    return cachedGenerators.get(degree);
  }

  public void encode(int[] toEncode, int ecBytes) {
    if (ecBytes == 0) {
      throw new IllegalArgumentException("No error correction bytes");
    }
    int dataBytes = toEncode.length - ecBytes;
    if (dataBytes <= 0) {
      throw new IllegalArgumentException("No data bytes provided");
    }
    DKG_GenericGFPoly generator = buildGenerator(ecBytes);
    int[] infoCoefficients = new int[dataBytes];
    System.arraycopy(toEncode, 0, infoCoefficients, 0, dataBytes);
    DKG_GenericGFPoly info = new DKG_GenericGFPoly(field, infoCoefficients);
    info = info.multiplyByMonomial(ecBytes, 1);
    DKG_GenericGFPoly remainder = info.divide(generator)[1];
    int[] coefficients = remainder.getCoefficients();
    int numZeroCoefficients = ecBytes - coefficients.length;
    for (int i = 0; i < numZeroCoefficients; i++) {
      toEncode[dataBytes + i] = 0;
    }
    System.arraycopy(coefficients, 0, toEncode, dataBytes + numZeroCoefficients, coefficients.length);
  }

}
