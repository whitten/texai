/*
 * PasswordGenerator.java
 *
 * Created on Jan 26, 2010, 11:10:10 AM
 *
 * Description: Generates random passwords.
 *
 * Copyright (C) Jan 26, 2010 reed.
 */
package org.texai.x509;

import net.jcip.annotations.NotThreadSafe;

/**
 * Generates random passwords.
 *
 * @author reed
 */
@NotThreadSafe
public final class PasswordGenerator {

  // printable ASCII characters
  private static final char[] PRINTABLE_ASCII = {
    '!', '\"', '#', '$', '%', '(', ')', '*', '+', '-', '.', '/',
    '\'', '[', '\\', ']', '^', '_', '`', '{', '|', '}', '~',
    '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', ':', '<', '=', '>', '?', '@',
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
  };

  /**
   * Constructs a new PasswordGenerator instance.
   */
  public PasswordGenerator() {
  }

  /**
   * Returns the number of possible combinations of generated random passwords for the given length.
   *
   * @param length the password length
   *
   * @return the number of possible combinations of generated random passwords for the given length
   */
  public double getNbrCombinations(final int length) {
    double nbrCombinations = 1.0d;
    final double characters = PRINTABLE_ASCII.length;
    for (int i = 0; i < length; i++) {
      nbrCombinations = nbrCombinations * characters;
    }
    return nbrCombinations;
  }

  /**
   * Generates a random password of the given length.
   *
   * @param length the password length
   *
   * @return a random password of the given length
   */
  public String generate(final int length) {
    final StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < length; i++) {
      stringBuilder.append(PRINTABLE_ASCII[X509Utils.getSecureRandom().nextInt(PRINTABLE_ASCII.length)]);
    }
    return stringBuilder.toString();
  }
}
