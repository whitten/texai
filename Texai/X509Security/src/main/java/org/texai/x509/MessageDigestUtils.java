package org.texai.x509;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import org.apache.log4j.Logger;
import org.texai.util.Base64Coder;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import static org.texai.x509.X509Utils.BOUNCY_CASTLE_PROVIDER;
import static org.texai.x509.X509Utils.addBouncyCastleSecurityProvider;

/**
 * MessageDigestUtils.java
 *
 * Description: Provides message digest, e.g. hashing, utilities.
 *
 * Copyright (C) Jan 19, 2015, Stephen L. Reed.
 */
public class MessageDigestUtils {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(MessageDigestUtils.class);

  /**
   * Prevents the construction of this static methods utility class.
   */
  private MessageDigestUtils() {
  }

  /**
   * Returns the SHA-512 hash of the given file, encoded as a base 64 string.
   *
   * @param filePath the file path
   *
   * @return the SHA-512 hash of the given file
   */
  public static String fileHashString(final String filePath) {
    //Preconditions
    assert StringUtils.isNonEmptyString(filePath) : "nodesPath must not be empty";

    return fileHashString(new File(filePath));
  }

  /**
   * Returns the SHA-512 hash of the given file, encoded as a base 64 string.
   *
   * @param file the given file
   *
   * @return the SHA-512 hash of the given file
   */
  public static String fileHashString(final File file) {
    //Preconditions
    assert file != null : "file must not be null";
    assert file.isFile() : "file must be a file, e.g. not a directory, " + file;

    final byte[] hashBytes;
    try {
      addBouncyCastleSecurityProvider();
      final MessageDigest messageDigest = MessageDigest.getInstance("SHA-512", BOUNCY_CASTLE_PROVIDER);
      messageDigest.reset();
      final FileInputStream fileInputStream = new FileInputStream(file);
      final byte[] dataBytes = new byte[1024];
      int nread;
      while ((nread = fileInputStream.read(dataBytes)) != -1) {
        messageDigest.update(dataBytes, 0, nread);
      }
      hashBytes = messageDigest.digest();
      return new String(Base64Coder.encode(hashBytes));
    } catch (NoSuchAlgorithmException | NoSuchProviderException | IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Returns the SHA-512 hash of the given bytes, encoded as a base 64 string.
   *
   * @param bytes the given bytes
   *
   * @return the SHA-512 hash of the given bytes
   */
  public static String bytesHashString(final byte[] bytes) {
    //Preconditions
    assert bytes != null : "bytes must not be null";

    try {
      addBouncyCastleSecurityProvider();
      final MessageDigest messageDigest = MessageDigest.getInstance("SHA-512", BOUNCY_CASTLE_PROVIDER);
      messageDigest.reset();
      final byte[] hashBytes = messageDigest.digest(bytes);
      return new String(Base64Coder.encode(hashBytes));
    } catch (NoSuchAlgorithmException | NoSuchProviderException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Verifies the expected SHA-512 hash of the given file.
   *
   * @param filePath the file path
   * @param fileHashString the file SHA-512 hash encoded as a base 64 string, used to detect tampering
   */
  public static void verifyFileHash(
          final String filePath,
          final String fileHashString) {
    //Preconditions
    assert StringUtils.isNonEmptyString(filePath) : "nodesPath must not be empty";
    assert StringUtils.isNonEmptyString(fileHashString) : "nodesFileHashString must not be empty";

    final String actualFileHashString = fileHashString(filePath);
    if (!actualFileHashString.equals(fileHashString)) {
      LOGGER.warn("actual encoded hash bytes:\n" + actualFileHashString);
      throw new TexaiException("file: " + filePath + " fails expected hash checksum");
    }
  }

}
