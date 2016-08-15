package com.alexjlockwood.example.submissionstatus;

/**
 * Utility class for math-related methods.
 */
final class MathUtils {

  /** Linear interpolate between a and b with parameter t. */
  public static float lerp(float a, float b, float t) {
    return a + (b - a) * t;
  }

  public static float cos(int degrees) {
    return (float) Math.cos(Math.toRadians(degrees));
  }

  public static float sin(int degrees) {
    return (float) Math.sin(Math.toRadians(degrees));
  }

  public static float sqrt(float value) {
    return (float) Math.sqrt(value);
  }

  private MathUtils() {}
}
