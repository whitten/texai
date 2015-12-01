package org.texai.x509;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 * SymmetricKeyUtils.java
 *
 * Description: Provides AES symmetric key utilities.
 *
 * Copyright (C) Dec 1, 2015, Stephen L. Reed.
 */
public class SymmetricKeyUtils {

  // the cipher object
  /**
   * Prevents the instantiation of this utility class.
   */
  private SymmetricKeyUtils() {
  }

  /**
   * Generates an AES secret key.
   *
   * @return the secret key
   */
  public static SecretKey generateKey() {
    final KeyGenerator keyGenerator;
    try {
      keyGenerator = KeyGenerator.getInstance("AES");
    } catch (NoSuchAlgorithmException ex) {
      throw new TexaiException(ex);
    }
    keyGenerator.init(256); // 128 default; 192 and 256 also possible
    return keyGenerator.generateKey();
  }

  /**
   * Saves the given secret key in the specified file.
   *
   * @param secretKey the given secret key
   * @param file the specified file
   */
  public static void saveKey(
          final SecretKey secretKey,
          final File file) {
    char[] hex = Hex.encodeHex(secretKey.getEncoded());
    try {
      writeStringToFile(file, String.valueOf(hex));
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Loads the secret key file.
   *
   * @param file the secret key file
   *
   * @return the secret key, or null if the file is not found
   */
  public static SecretKey loadKey(final File file) {
    //Preconditions
    assert file != null : "file must not be null";

    final String data;
    try {
      data = new String(readFileToByteArray(file));
    } catch (FileNotFoundException ex) {
      return null;
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
    final byte[] encoded;
    try {
      encoded = Hex.decodeHex(data.toCharArray());
    } catch (DecoderException ex) {
      throw new TexaiException(ex);
    }
    return new SecretKeySpec(encoded, "AES");
  }

  /** Encrypt the given plain text using the symmetic AES algorithm, and the given secret key.
   *
   * @param plainTextBytes the given plain text bytes
   * @param secretKey the given secret key
   * @return the encrypted text bytes
   */
  public static byte[] encrypt(
          final byte[] plainTextBytes,
          final SecretKey secretKey) {
    //Preconditions
    assert plainTextBytes != null : "plainText must not be null";
    assert secretKey != null : "secretKey must not be null";

    final Cipher cipher;
    try {
      cipher = Cipher.getInstance("AES");
    } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
      throw new TexaiException(ex);
    }
    try {
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);
    } catch (InvalidKeyException ex) {
      throw new TexaiException(ex);
    }
    byte[] encryptedBytes;
    try {
      encryptedBytes = cipher.doFinal(plainTextBytes);
    } catch (IllegalBlockSizeException | BadPaddingException ex) {
      throw new TexaiException(ex);
    }
    return encryptedBytes;
  }

  /** Decrypt the given encrypted text using the symmetric AES algorithm, and the given secret key.
   *
   * @param encryptedTextBytes the given encrypted text bytes
   * @param secretKey the given secret key
   * @return the plain text bytes
   */
  public static byte[] decrypt(
          final byte[] encryptedTextBytes,
          final SecretKey secretKey) {
    //Preconditions
    assert encryptedTextBytes != null : "encryptedTextBytes must not be null";
    assert secretKey != null : "secretKey must not be null";

    final Cipher cipher;
    try {
      cipher = Cipher.getInstance("AES");
    } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
      throw new TexaiException(ex);
    }
    try {
      cipher.init(Cipher.DECRYPT_MODE, secretKey);
    } catch (InvalidKeyException ex) {
      throw new TexaiException(ex);
    }
    byte[] decryptedBytes;
    try {
      decryptedBytes = cipher.doFinal(encryptedTextBytes);
    } catch (IllegalBlockSizeException | BadPaddingException ex) {
      throw new TexaiException(ex);
    }
    return decryptedBytes;
  }

  /** Encrypt the given plain text using the symmetic AES algorithm, base 64 encoding, and the given secret key.
   *
   * @param plainText the given plain text
   * @param secretKey the given secret key
   * @return the encrypted text
   */
  public static String encryptBase64(
          final String plainText,
          final SecretKey secretKey) {
    //Preconditions
    assert StringUtils.isNonEmptyString(plainText) : "plainText must be a non-empty string";
    assert secretKey != null : "secretKey must not be null";

    final Cipher cipher;
    try {
      cipher = Cipher.getInstance("AES");
    } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
      throw new TexaiException(ex);
    }
    byte[] plainTextBytes = plainText.getBytes();
    try {
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);
    } catch (InvalidKeyException ex) {
      throw new TexaiException(ex);
    }
    byte[] encryptedBytes;
    try {
      encryptedBytes = cipher.doFinal(plainTextBytes);
    } catch (IllegalBlockSizeException | BadPaddingException ex) {
      throw new TexaiException(ex);
    }
    Base64.Encoder encoder = Base64.getEncoder();
    String encryptedText = encoder.encodeToString(encryptedBytes);
    return encryptedText;
  }

  /** Decrypt the given encrypted text using the symmetric AES algorithm, base 64 decoding, and the given secret key.
   *
   * @param encryptedText the given encrypted text
   * @param secretKey the given secret key
   * @return the plain text
   */
  public static String decryptBase64(
          final String encryptedText,
          final SecretKey secretKey) {
    //Preconditions
    assert StringUtils.isNonEmptyString(encryptedText) : "encryptedText must be a non-empty string";
    assert secretKey != null : "secretKey must not be null";

    final Cipher cipher;
    try {
      cipher = Cipher.getInstance("AES");
    } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
      throw new TexaiException(ex);
    }
    Base64.Decoder decoder = Base64.getDecoder();
    byte[] encryptedTextBytes = decoder.decode(encryptedText);
    try {
      cipher.init(Cipher.DECRYPT_MODE, secretKey);
    } catch (InvalidKeyException ex) {
      throw new TexaiException(ex);
    }
    byte[] decryptedBytes;
    try {
      decryptedBytes = cipher.doFinal(encryptedTextBytes);
    } catch (IllegalBlockSizeException | BadPaddingException ex) {
      throw new TexaiException(ex);
    }
    String decryptedText = new String(decryptedBytes);
    return decryptedText;
  }
}
