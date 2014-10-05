/*
 * OneWayEncryptionService.java
 *
 * Created on Apr 21, 2009, 2:57:10 PM
 *
 * Description: Provides a singleton one-way encryption service.
 *
 * Copyright (C) Apr 21, 2009 Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.jcip.annotations.ThreadSafe;

/**
 * Provides a singleton one-way encryption service. Adapted from James Shvarts' article "Password Encryption: Rationale and Java Example"
 * http://www.devarticles.com/c/a/Java/Password-Encryption-Rationale-and-Java-Example
 *
 * The Sun BASE64Encoder class has been replaced with an open source equivalent.
 *
 * @author Stephen L. Reed
 */
@ThreadSafe
public final class OneWayEncryptionService {

  // the singleton one-way encryption service
  private static OneWayEncryptionService instance;
  // the SHA-256 message digest
  final MessageDigest messageDigest;

  // Privately constructs a new one-way encryption service instance.
  private OneWayEncryptionService() {
    try {
      messageDigest = MessageDigest.getInstance("SHA-512");
      messageDigest.reset();
    } catch (NoSuchAlgorithmException e) {
      throw new TexaiException(e);
    }
  }

  /**
   * Encrypts the given plaintext.
   *
   * @param plaintext the plaintext
   *
   * @return the encrypted text
   */
  @Deprecated
  public synchronized String encrypt(final String plaintext) {
    //Preconditions
    assert StringUtils.isNonEmptyString(plaintext) : "plaintext must not be empty";

    try {
      messageDigest.reset();
      messageDigest.update(plaintext.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException ex) {
      throw new TexaiException(ex);
    }
    final byte[] raw = messageDigest.digest();
    return new String(Base64Coder.encode(raw));
  }

  /**
   * Encrypts the given plaintext with a given salt that prevents dictionary attacks.
   *
   * @param plaintext the plaintext
   * @param salt the random value stored with the cipher text for subsequent verification
   *
   * @return the encrypted text
   */
  public synchronized String encrypt(final String plaintext, final String salt) {
    //Preconditions
    assert StringUtils.isNonEmptyString(plaintext) : "plaintext must not be empty";
    assert StringUtils.isNonEmptyString(salt) : "salt must not be empty";

    try {
      messageDigest.reset();
      messageDigest.update(plaintext.getBytes("UTF-8"));
      messageDigest.update(salt.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException ex) {
      throw new TexaiException(ex);
    }
    final byte[] raw = messageDigest.digest();
    return new String(Base64Coder.encode(raw));
  }

  /**
   * Encrypt the given number.
   *
   * @param nbr the number to be encrypted
   *
   * @return the encrypted text
   */
  @Deprecated
  public synchronized String encrypt(final long nbr) {
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    final DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
    try {
      dataOutputStream.writeLong(nbr);
      dataOutputStream.flush();
    } catch (final IOException ex) {
      throw new TexaiException(ex);
    }
    messageDigest.reset();
    messageDigest.update(byteArrayOutputStream.toByteArray());
    final byte[] raw = messageDigest.digest();
    return new String(Base64Coder.encode(raw));
  }

  /**
   * Encrypt the given number with a given salt that prevents dictionary attacks.
   *
   * @param nbr the number to be encrypted
   * @param salt the random value stored with the cipher text for subsequent verification
   *
   * @return the encrypted text
   */
  public synchronized String encrypt(final long nbr, final String salt) {
    //Preconditions
    assert StringUtils.isNonEmptyString(salt) : "salt must not be empty";

    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    final DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
    try {
      dataOutputStream.writeLong(nbr);
      dataOutputStream.flush();
    } catch (final IOException ex) {
      throw new TexaiException(ex);
    }
    try {
      messageDigest.reset();
      messageDigest.update(byteArrayOutputStream.toByteArray());
      messageDigest.update(salt.getBytes("UTF-8"));
      final byte[] raw = messageDigest.digest();
      return new String(Base64Coder.encode(raw));
    } catch (UnsupportedEncodingException e) {
      throw new TexaiException(e);
    }
  }

  /**
   * Returns the static instance.
   *
   * @return the singleton one-way encryption service
   */
  public static synchronized OneWayEncryptionService getInstance() {
    if (instance == null) {
      return new OneWayEncryptionService();
    } else {
      return instance;
    }
  }
}
