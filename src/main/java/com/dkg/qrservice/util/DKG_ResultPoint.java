package com.dkg.qrservice.util;

public class DKG_ResultPoint {

  private final float x;
  private final float y;

  public DKG_ResultPoint(float x, float y) {
    this.x = x;
    this.y = y;
  }

  public final float getX() {
    return x;
  }

  public final float getY() {
    return y;
  }

  @Override
  public final boolean equals(Object other) {
    if (other instanceof DKG_ResultPoint) {
      DKG_ResultPoint otherPoint = (DKG_ResultPoint) other;
      return x == otherPoint.x && y == otherPoint.y;
    }
    return false;
  }

  @Override
  public final int hashCode() {
    return 31 * Float.floatToIntBits(x) + Float.floatToIntBits(y);
  }

  @Override
  public final String toString() {
    return "(" + x + ',' + y + ')';
  }


  public static void orderBestPatterns(DKG_ResultPoint[] patterns) {

    // Find distances between pattern centers
    float zeroOneDistance = distance(patterns[0], patterns[1]);
    float oneTwoDistance = distance(patterns[1], patterns[2]);
    float zeroTwoDistance = distance(patterns[0], patterns[2]);

    DKG_ResultPoint pointA;
    DKG_ResultPoint pointB;
    DKG_ResultPoint pointC;
    // Assume one closest to other two is B; A and C will just be guesses at first
    if (oneTwoDistance >= zeroOneDistance && oneTwoDistance >= zeroTwoDistance) {
      pointB = patterns[0];
      pointA = patterns[1];
      pointC = patterns[2];
    } else if (zeroTwoDistance >= oneTwoDistance && zeroTwoDistance >= zeroOneDistance) {
      pointB = patterns[1];
      pointA = patterns[0];
      pointC = patterns[2];
    } else {
      pointB = patterns[2];
      pointA = patterns[0];
      pointC = patterns[1];
    }

    // Use cross product to figure out whether A and C are correct or flipped.
    // This asks whether BC x BA has a positive z component, which is the arrangement
    // we want for A, B, C. If it's negative, then we've got it flipped around and
    // should swap A and C.
    if (crossProductZ(pointA, pointB, pointC) < 0.0f) {
      DKG_ResultPoint temp = pointA;
      pointA = pointC;
      pointC = temp;
    }

    patterns[0] = pointA;
    patterns[1] = pointB;
    patterns[2] = pointC;
  }


  public static float distance(DKG_ResultPoint pattern1, DKG_ResultPoint pattern2) {
    return DKG_MathUtils.distance(pattern1.x, pattern1.y, pattern2.x, pattern2.y);
  }


  private static float crossProductZ(DKG_ResultPoint pointA,
                                     DKG_ResultPoint pointB,
                                     DKG_ResultPoint pointC) {
    float bX = pointB.x;
    float bY = pointB.y;
    return ((pointC.x - bX) * (pointA.y - bY)) - ((pointC.y - bY) * (pointA.x - bX));
  }

}
