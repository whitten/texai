/*
 * SerializableObjectSigner.java
 *
 * Created on Apr 13, 2010, 3:08:42 PM
 *
 * Description: Provides digital signatures for serializable objects.
 *
 * Copyright (C) Apr 13, 2010, Stephen L. Reed.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import net.jcip.annotations.NotThreadSafe;

/**
 * Provides digital signatures for serializable objects.
 *
 * @author reed
 */
@NotThreadSafe
public final class SerializableObjectSigner {

  /**
   * Prevents this utility class from being instantiated.
   */
  private SerializableObjectSigner() {
  }

  /**
   * Signs the given serializable object and returns the digital signature bytes.
   *
   * @param serializableObject the serializable object
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
          final Serializable serializableObject,
          final PrivateKey privateKey)
          throws
          NoSuchAlgorithmException,
          InvalidKeyException,
          IOException,
          SignatureException {
    //Preconditions
    assert serializableObject != null : "pathName must not be null";
    assert privateKey != null : "privateKey must not be null";

    final Signature signature = Signature.getInstance(X509Utils.DIGITAL_SIGNATURE_ALGORITHM);
    signature.initSign(privateKey);
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    objectOutputStream.writeObject(serializableObject);
    signature.update(byteArrayOutputStream.toByteArray());
    return signature.sign();
  }

  /**
   * Returns whether the given signature bytes verify the given serializable object.
   *
   * @param serializableObject the serializable object
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
          final Serializable serializableObject,
          final X509Certificate x509Certificate,
          final byte[] signatureBytes)
          throws
          NoSuchAlgorithmException,
          InvalidKeyException,
          IOException,
          SignatureException {
    //Preconditions
    assert serializableObject != null : "serializableObject must not be null";
    assert x509Certificate != null : "x509Certificate must not be null";
    assert signatureBytes.length > 0 : "signatureBytes must not be empty";

    final Signature signature = Signature.getInstance(X509Utils.DIGITAL_SIGNATURE_ALGORITHM);
    signature.initVerify(x509Certificate.getPublicKey());
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    objectOutputStream.writeObject(serializableObject);
    signature.update(byteArrayOutputStream.toByteArray());
    return signature.verify(signatureBytes);
  }
  }
