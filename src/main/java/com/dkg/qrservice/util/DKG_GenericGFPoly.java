package com.dkg.qrservice.util;

final class DKG_GenericGFPoly {

  private final DKG_GenericGF field;
  private final int[] coefficients;

  DKG_GenericGFPoly(DKG_GenericGF field, int[] coefficients) {
    if (coefficients.length == 0) {
      throw new IllegalArgumentException();
    }
    this.field = field;
    int coefficientsLength = coefficients.length;
    if (coefficientsLength > 1 && coefficients[0] == 0) {
      // Leading term must be non-zero for anything except the constant polynomial "0"
      int firstNonZero = 1;
      while (firstNonZero < coefficientsLength && coefficients[firstNonZero] == 0) {
        firstNonZero++;
      }
      if (firstNonZero == coefficientsLength) {
        this.coefficients = new int[]{0};
      } else {
        this.coefficients = new int[coefficientsLength - firstNonZero];
        System.arraycopy(coefficients,
            firstNonZero,
            this.coefficients,
            0,
            this.coefficients.length);
      }
    } else {
      this.coefficients = coefficients;
    }
  }

  int[] getCoefficients() {
    return coefficients;
  }


  int getDegree() {
    return coefficients.length - 1;
  }


  boolean isZero() {
    return coefficients[0] == 0;
  }


  int getCoefficient(int degree) {
    return coefficients[coefficients.length - 1 - degree];
  }

  int evaluateAt(int a) {
    if (a == 0) {
      // Just return the x^0 coefficient
      return getCoefficient(0);
    }
    if (a == 1) {
      // Just the sum of the coefficients
      int result = 0;
      for (int coefficient : coefficients) {
        result = DKG_GenericGF.addOrSubtract(result, coefficient);
      }
      return result;
    }
    int result = coefficients[0];
    int size = coefficients.length;
    for (int i = 1; i < size; i++) {
      result = DKG_GenericGF.addOrSubtract(field.multiply(a, result), coefficients[i]);
    }
    return result;
  }

  DKG_GenericGFPoly addOrSubtract(DKG_GenericGFPoly other) {
    if (!field.equals(other.field)) {
      throw new IllegalArgumentException("GenericGFPolys do not have same GenericGF field");
    }
    if (isZero()) {
      return other;
    }
    if (other.isZero()) {
      return this;
    }

    int[] smallerCoefficients = this.coefficients;
    int[] largerCoefficients = other.coefficients;
    if (smallerCoefficients.length > largerCoefficients.length) {
      int[] temp = smallerCoefficients;
      smallerCoefficients = largerCoefficients;
      largerCoefficients = temp;
    }
    int[] sumDiff = new int[largerCoefficients.length];
    int lengthDiff = largerCoefficients.length - smallerCoefficients.length;
    // Copy high-order terms only found in higher-degree polynomial's coefficients
    System.arraycopy(largerCoefficients, 0, sumDiff, 0, lengthDiff);

    for (int i = lengthDiff; i < largerCoefficients.length; i++) {
      sumDiff[i] = DKG_GenericGF.addOrSubtract(smallerCoefficients[i - lengthDiff], largerCoefficients[i]);
    }

    return new DKG_GenericGFPoly(field, sumDiff);
  }

  DKG_GenericGFPoly multiply(DKG_GenericGFPoly other) {
    if (!field.equals(other.field)) {
      throw new IllegalArgumentException("GenericGFPolys do not have same GenericGF field");
    }
    if (isZero() || other.isZero()) {
      return field.getZero();
    }
    int[] aCoefficients = this.coefficients;
    int aLength = aCoefficients.length;
    int[] bCoefficients = other.coefficients;
    int bLength = bCoefficients.length;
    int[] product = new int[aLength + bLength - 1];
    for (int i = 0; i < aLength; i++) {
      int aCoeff = aCoefficients[i];
      for (int j = 0; j < bLength; j++) {
        product[i + j] = DKG_GenericGF.addOrSubtract(product[i + j],
            field.multiply(aCoeff, bCoefficients[j]));
      }
    }
    return new DKG_GenericGFPoly(field, product);
  }

  DKG_GenericGFPoly multiply(int scalar) {
    if (scalar == 0) {
      return field.getZero();
    }
    if (scalar == 1) {
      return this;
    }
    int size = coefficients.length;
    int[] product = new int[size];
    for (int i = 0; i < size; i++) {
      product[i] = field.multiply(coefficients[i], scalar);
    }
    return new DKG_GenericGFPoly(field, product);
  }

  DKG_GenericGFPoly multiplyByMonomial(int degree, int coefficient) {
    if (degree < 0) {
      throw new IllegalArgumentException();
    }
    if (coefficient == 0) {
      return field.getZero();
    }
    int size = coefficients.length;
    int[] product = new int[size + degree];
    for (int i = 0; i < size; i++) {
      product[i] = field.multiply(coefficients[i], coefficient);
    }
    return new DKG_GenericGFPoly(field, product);
  }

  DKG_GenericGFPoly[] divide(DKG_GenericGFPoly other) {
    if (!field.equals(other.field)) {
      throw new IllegalArgumentException("GenericGFPolys do not have same GenericGF field");
    }
    if (other.isZero()) {
      throw new IllegalArgumentException("Divide by 0");
    }

    DKG_GenericGFPoly quotient = field.getZero();
    DKG_GenericGFPoly remainder = this;

    int denominatorLeadingTerm = other.getCoefficient(other.getDegree());
    int inverseDenominatorLeadingTerm = field.inverse(denominatorLeadingTerm);

    while (remainder.getDegree() >= other.getDegree() && !remainder.isZero()) {
      int degreeDifference = remainder.getDegree() - other.getDegree();
      int scale = field.multiply(remainder.getCoefficient(remainder.getDegree()), inverseDenominatorLeadingTerm);
      DKG_GenericGFPoly term = other.multiplyByMonomial(degreeDifference, scale);
      DKG_GenericGFPoly iterationQuotient = field.buildMonomial(degreeDifference, scale);
      quotient = quotient.addOrSubtract(iterationQuotient);
      remainder = remainder.addOrSubtract(term);
    }

    return new DKG_GenericGFPoly[] { quotient, remainder };
  }

  @Override
  public String toString() {
    if (isZero()) {
      return "0";
    }
    StringBuilder result = new StringBuilder(8 * getDegree());
    for (int degree = getDegree(); degree >= 0; degree--) {
      int coefficient = getCoefficient(degree);
      if (coefficient != 0) {
        if (coefficient < 0) {
          if (degree == getDegree()) {
            result.append("-");
          } else {
            result.append(" - ");
          }
          coefficient = -coefficient;
        } else {
          if (result.length() > 0) {
            result.append(" + ");
          }
        }
        if (degree == 0 || coefficient != 1) {
          int alphaPower = field.log(coefficient);
          if (alphaPower == 0) {
            result.append('1');
          } else if (alphaPower == 1) {
            result.append('a');
          } else {
            result.append("a^");
            result.append(alphaPower);
          }
        }
        if (degree != 0) {
          if (degree == 1) {
            result.append('x');
          } else {
            result.append("x^");
            result.append(degree);
          }
        }
      }
    }
    return result.toString();
  }

}
