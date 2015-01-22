/*
 * FileSigner.java
 *
 * Created on Jan 27, 2010, 1:08:52 AM
 *
 * Description: Provides digital signatures for files.
 *
 * Copyright (C) Jan 27, 2010 reed.
 */
package org.texai.x509;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import net.jcip.annotations.NotThreadSafe;

/**
 * Provides digital signatures for files.
 *
 * @author reed
 */
@NotThreadSafe
public final class FileSigner {

  /**
   * Prevents this utility class from being instantiated.
   */
  private FileSigner() {
  }

  /**
   * Signs the given file and returns the digital signature bytes.
   *
   * @param pathName the file path name
   * @param privateKey the sender's private key
   *
   * @return the digital signature bytes
   *
   * @throws NoSuchAlgorithmException when the signature algorithm is invalid
   * @throws InvalidKeyException if there is an error with the private key
   * @throws IOException if an input/output error occurs
   * @throws SignatureException if there is a problem with the signature
   */
  public static byte[] sign(
          final String pathName,
          final PrivateKey privateKey)
          throws
          NoSuchAlgorithmException,
          InvalidKeyException,
          IOException,
          SignatureException {
    //Preconditions
    assert pathName != null : "pathName must not be null";
    assert !pathName.isEmpty() : "pathName must not be empty";
    assert privateKey != null : "privateKey must not be null";

    final Signature signature = Signature.getInstance(X509Utils.DIGITAL_SIGNATURE_ALGORITHM);
    signature.initSign(privateKey);
    try (FileInputStream fileInputStream = new FileInputStream(pathName)) {
      final byte[] dataBytes = new byte[1024];
      int nbrBytesRead = fileInputStream.read(dataBytes);
      while (nbrBytesRead > 0) {
        signature.update(dataBytes, 0, nbrBytesRead);
        nbrBytesRead = fileInputStream.read(dataBytes);
      }
    }
    return signature.sign();
  }

  /**
   * Returns whether the given signature bytes verify the given file.
   *
   * @param pathName the file path name
   * @param x509Certificate the sender's X.509 certificate, that contains the public key
   * @param signatureBytes the signature bytes
   *
   * @return whether the given signature bytes verify the given file
   *
   * @throws NoSuchAlgorithmException when the signature algorithm is invalid
   * @throws InvalidKeyException if there is an error with the private key
   * @throws IOException if an input/output error occurs
   * @throws SignatureException if there is a problem with the signature
   */
  public static boolean verify(
          final String pathName,
          final X509Certificate x509Certificate,
          final byte[] signatureBytes)
          throws
          NoSuchAlgorithmException,
          InvalidKeyException,
          IOException,
          SignatureException {
    //Preconditions
    assert pathName != null : "pathName must not be null";
    assert !pathName.isEmpty() : "pathName must not be empty";
    assert x509Certificate != null : "x509Certificate must not be null";
    assert signatureBytes.length > 0 : "signatureBytes must not be empty";

    final Signature signature = Signature.getInstance(X509Utils.DIGITAL_SIGNATURE_ALGORITHM);
    signature.initVerify(x509Certificate.getPublicKey());
    try (FileInputStream fileInputStream = new FileInputStream(pathName)) {
      final byte[] dataBytes = new byte[1024];
      int nbrBytesRead = fileInputStream.read(dataBytes);
      while (nbrBytesRead > 0) {
        signature.update(dataBytes, 0, nbrBytesRead);
        nbrBytesRead = fileInputStream.read(dataBytes);
      }
    }
    return signature.verify(signatureBytes);
  }
}
