/*
 * FileSigner.java
 *
 * Created on Jan 27, 2010, 1:08:52 AM
 *
 * Description: Provides digital signatures for files.
 *
 * Copyright (C) Jan 27, 2010 reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
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

/** Provides digital signatures for files.
 *
 * @author reed
 */
@NotThreadSafe
public final class FileSigner {

  /** Prevents this utility class from being instantiated. */
  private FileSigner() {
  }

  /** Signs the given file and returns the digital signature bytes.
   *
   * @param pathName the file path name
   * @param privateKey the sender's private key
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

  /** Returns whether the given signature bytes verify the given file.
   *
   * @param pathName the file path name
   * @param x509Certificate the sender's X.509 certificate, that contains the public key
   * @param signatureBytes the signature bytes
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
