/*
 * X509SecurityInfo.java
 *
 * Created on Mar 1, 2010, 11:52:56 AM
 *
 * Description: Provides a container for X509 security information.
 *
 * Copyright (C) Mar 1, 2010 reed.
 */
package org.texai.x509;

import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import net.jcip.annotations.Immutable;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 * Provides a container for X509 security information.
 *
 * @author reed
 */
@Immutable
public final class X509SecurityInfo {

  // the key manager factory
  private final KeyManagerFactory keyManagerFactory;
  // the key store
  private final KeyStore keyStore;
  // the cached private key entry that contains the self-signed X.509 certificate and private key
  private PrivateKeyEntry privateKeyEntry;


  /**
   * Constructs a new X509SecurityInfo instance.
   *
   * @param keyStore the key store containing the peer's X.509 certificate chain
   * @param keyStorePassword the password to the key store
   * @param certificateAlias the certificate entry alias
   */
  public X509SecurityInfo(
          final KeyStore keyStore,
          final char[] keyStorePassword,
          final String certificateAlias) {
    //Preconditions
    assert keyStore != null : "keyStore must not be null";
    assert keyStorePassword != null : "keyStorePassword must not be null";
    assert StringUtils.isNonEmptyString(certificateAlias) : "certificateAlias must be a non-empty string";

    this.keyStore = keyStore;
    try {
      keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
      keyManagerFactory.init(keyStore, keyStorePassword);
      try {
      privateKeyEntry = (PrivateKeyEntry) keyStore.getEntry(certificateAlias, new PasswordProtection(keyStorePassword));
      } catch (UnsupportedOperationException ex) {
        // trusted public certificate without a private key
        privateKeyEntry = null;
      }
    } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableEntryException ex) {
      throw new TexaiException(ex);
    }

//    int length = ((X509Certificate[]) privateKeyEntry.getCertificateChain()).length;
//    assert length == 1 : "X.509 certificate chain should have length 1, but was " + length;
  }

  /**
   * Gets the key store.
   *
   * @return the key store
   */
  public KeyStore getKeyStore() {
    return keyStore;
  }

  /**
   * Returns one key manager for each type of key material.
   *
   * @return the key managers
   */
  public KeyManager[] getKeyManagers() {
    return keyManagerFactory.getKeyManagers();
  }

  /** Returns whether the certificate is public, and therefore does not contain its private key.
   * @return whether the certificate is public, and therefore does not contain its private key
   */
  public boolean isPublicCertificate() {
    return privateKeyEntry == null;
  }

  /**
   * Returns the private key.
   *
   * @return the private key
   */
  public PrivateKey getPrivateKey() {
    return privateKeyEntry.getPrivateKey();
  }

  /**
   * Returns the certificate chain.
   *
   * @return the certificate chain
   */
  public X509Certificate[] getCertificateChain() {
    final X509Certificate[] certificateChain;
    certificateChain = (X509Certificate[]) privateKeyEntry.getCertificateChain();

    //Postconditions
    assert certificateChain != null;
    //assert certificateChain.length == 1 : "X.509 certificate chain should have length 1, but was " + certificateChain.length;

    return certificateChain;
  }

  /**
   * Returns the X.509 certificate.
   *
   * @return the X.509 certificate
   */
  public X509Certificate getX509Certificate() {
    return getCertificateChain()[0];
  }

  /**
   * Returns the certificate path.
   *
   * @return the certificate path
   */
  public CertPath getCertPath() {
    final X509Certificate[] certificateChain = getCertificateChain();
    final CertificateFactory certificateFactory;
    try {
      certificateFactory = CertificateFactory.getInstance("X.509", X509Utils.BOUNCY_CASTLE_PROVIDER);
      final List<Certificate> certificateList = new ArrayList<>();
      // the certificate path does not include the trust anchor which terminates the certificate chain
      for (int i = 0; i < certificateChain.length - 1; i++) {
        certificateList.add(certificateChain[i]);
      }
      return certificateFactory.generateCertPath(certificateList);
    } catch (CertificateException | NoSuchProviderException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "[X509 certificate information for ...\n" + getX509Certificate().toString() + "]";
  }

  /**
   * Returns whether the X509 certificate can be used for digital signing.
   *
   * @return the X509 certificate can be used for digital signing
   */
  public boolean isDigitialSigniatureCertificate() {
    return X509Utils.hasKeyUsage(getCertificateChain()[0], KeyUsage.digitalSignature);

  }
}
