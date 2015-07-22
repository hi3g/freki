package se.tre.freki.labels;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.CharMatcher;

/**
 * Utility methods for labels.
 */
public class Labels {
  /** A Guava character matcher that finds illegal characters. */
  private static final CharMatcher INVALID_LETTER_MATCHER =
      CharMatcher.anyOf("-./_")
          .or(CharMatcher.JAVA_UPPER_CASE)
          .or(CharMatcher.JAVA_LOWER_CASE)
          .or(CharMatcher.JAVA_DIGIT)
          .and(CharMatcher.ASCII)
          .negate()
          .precomputed();

  /**
   * Ensures that a given string is a valid metric name or tag name/value.
   *
   * @param what A human readable description of what's being validated.
   * @param name The string to validate.
   * @throws IllegalArgumentException if the string isn't valid.
   */
  public static void checkLabelName(final String what, final String name) {
    checkNotNull(name, "%s must not be null", what);
    final int index = invalidIndexIn(name);

    if (-1 != index) {
      final char invalidChar = name.charAt(index);
      throw new IllegalArgumentException(
          "Illegal character in the " + what + " \"" + name + "\". Character \"" + invalidChar
          + "\" (code: " + (int) invalidChar + ") is not allowed");
    }
  }

  /**
   * Calculate the index of the first invalid character in the provided label name.
   *
   * @param labelName The label name to calculate the invalid character in
   * @return the index of the invalid character or {@code -1} if no character was invalid
   */
  public static int invalidIndexIn(final CharSequence labelName) {
    return INVALID_LETTER_MATCHER.indexIn(labelName);
  }
}
