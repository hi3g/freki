package se.tre.freki.labels;

import static org.junit.Assert.*;

import org.junit.Test;

public class LabelsTest {
  @Test
  public void validateLabelName() {
    final String validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUWVXYZ0123456789-_./";

    for (char c = 0; c < 255; ++c) {
      final String input = String.valueOf(c);
      try {
        Labels.checkLabelName("test", input);
        assertTrue("character " + input.charAt(0) + " with code " + ((int) input.charAt(
                0)) + " is not in the valid chars",
            validChars.contains(input));
      } catch (IllegalArgumentException e) {
        assertFalse(validChars.contains(input));
      }
    }
  }

  @Test(expected = NullPointerException.class)
  public void validateLabelNameNullString() {
    Labels.checkLabelName("test", null);
  }
}