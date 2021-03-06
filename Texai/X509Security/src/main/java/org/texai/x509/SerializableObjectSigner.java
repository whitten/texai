/*
 * SerializableObjectSigner.java
 *
 * Created on Apr 13, 2010, 3:08:42 PM
 *
 * Description: Provides digital signatures for serializable objects.
 *
 * Copyright (C) Apr 13, 2010, Stephen L. Reed.
 */
package org.texai.x509;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import net.jcip.annotations.NotThreadSafe;
import org.texai.util.TexaiException;
import static org.texai.x509.X509Utils.BOUNCY_CASTLE_PROVIDER;
import static org.texai.x509.X509Utils.addBouncyCastleSecurityProvider;

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
   */
  public static boolean verify(
          final Serializable serializableObject,
          final X509Certificate x509Certificate,
          final byte[] signatureBytes)
          throws
          NoSuchAlgorithmException,
          InvalidKeyException,
          IOException {
    //Preconditions
    assert serializableObject != null : "serializableObject must not be null";
    assert x509Certificate != null : "x509Certificate must not be null";
    assert signatureBytes.length > 0 : "signatureBytes must not be empty";

    final Signature signature = Signature.getInstance(X509Utils.DIGITAL_SIGNATURE_ALGORITHM);
    signature.initVerify(x509Certificate.getPublicKey());
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    objectOutputStream.writeObject(serializableObject);
    try {
      signature.update(byteArrayOutputStream.toByteArray());
      return signature.verify(signatureBytes);
    } catch (SignatureException ex) {
      return false;
    }
  }

  /**
   * Returns a SHA-512 hash of the given ArrayList of serializable objects.
   *
   * @param serializableObject the serialized object, which is an ArrayList of serializable objects.
   *
   * @return a SHA-512 hash
   */
  public static byte[] sha512Hash(final ArrayList<Object> serializableObject) {
    //Preconditions
    assert serializableObject != null : "serializableObject must not be null";
    assert !serializableObject.isEmpty() : "serializableObject must not be empty";

    try {
      addBouncyCastleSecurityProvider();
      final MessageDigest messageDigest = MessageDigest.getInstance("SHA-512", BOUNCY_CASTLE_PROVIDER);
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      objectOutputStream.writeObject(serializableObject);
      messageDigest.update(byteArrayOutputStream.toByteArray());
      return messageDigest.digest();
    } catch (NoSuchAlgorithmException | NoSuchProviderException | IOException ex) {
      throw new TexaiException(ex);
    }
  }

}
