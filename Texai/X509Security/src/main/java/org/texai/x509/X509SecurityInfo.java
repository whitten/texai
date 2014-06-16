/*
 * X509SecurityInfo.java
 *
 * Created on Mar 1, 2010, 11:52:56 AM
 *
 * Description: Provides a container for X509 security information.
 *
 * Copyright (C) Mar 1, 2010 reed.
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
import javax.net.ssl.X509KeyManager;
import net.jcip.annotations.Immutable;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.texai.util.TexaiException;

/**  Provides a container for X509 security information.
 *
 * @author reed
 */
@Immutable
public final class X509SecurityInfo {

  /** the key store containing the trusted Texai X.509 certificate */
  private final KeyStore trustStore;
  /** the key manager factory */
  private final KeyManagerFactory keyManagerFactory;
  /** the key store */
  private final KeyStore keyStore;
  /** the private key entry for a certain alias id that contains the X.509 certificate and private key for that id */
  private final PrivateKeyEntry privateKeyEntry;

  /** Constructs a new X509SecurityInfo instance.
   *
   * @param trustStore the key store containing the trusted Texai X.509 certificate
   * @param keyStore the key store containing the peer's X.509 certificate chain
   * @param keyStorePassword the password to the key store
   * @param alias the private key entry alias
   */
  public X509SecurityInfo(
          final KeyStore trustStore,
          final KeyStore keyStore,
          final char[] keyStorePassword,
          final String alias) {
    //Preconditions
    assert trustStore != null : "trustStore must not be null";
    assert keyStore != null : "keyStore must not be null";
    assert keyStorePassword != null : "keyStorePassword must not be null";

    this.trustStore = trustStore;
    this.keyStore = keyStore;
    try {
      keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
      keyManagerFactory.init(keyStore, keyStorePassword);
      if (alias == null) {
        privateKeyEntry = null;
      } else {
        privateKeyEntry = (PrivateKeyEntry) keyStore.getEntry(alias, new PasswordProtection(keyStorePassword));
        assert privateKeyEntry != null : "privateKeyEntry not found for alias " + alias;
      }
    } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableEntryException ex) {
      throw new TexaiException(ex);
    }

  }

  /** Gets the key store containing the trusted Texai X.509 certificate.
   *
   * @return the trustStore
   */
  public KeyStore getTrustStore() {
    return trustStore;
  }

  /** Gets the key store.
   *
   * @return the key store
   */
  public KeyStore getKeyStore() {
    return keyStore;
  }

  /** Returns one key manager for each type of key material.
   *
   * @return the key managers
   */
  public KeyManager[] getKeyManagers() {
    return keyManagerFactory.getKeyManagers();
  }

  /** Returns the private key.
   *
   * @return the private key
   */
  public PrivateKey getPrivateKey() {
    if (privateKeyEntry == null) {
      // for unit tests
      final X509KeyManager x509KeyManager = (X509KeyManager) getKeyManagers()[0];
      return x509KeyManager.getPrivateKey(X509Utils.ENTRY_ALIAS);
    } else {
      return privateKeyEntry.getPrivateKey();
    }
  }

  /** Returns the certificate chain.
   *
   * @return the certificate chain
   */
  public X509Certificate[] getCertificateChain() {
    final X509Certificate[] certificateChain;
    if (privateKeyEntry == null) {
      // for unit tests
      final X509KeyManager x509KeyManager = (X509KeyManager) getKeyManagers()[0];
      certificateChain = x509KeyManager.getCertificateChain(X509Utils.ENTRY_ALIAS);
    } else {
      certificateChain =  (X509Certificate[]) privateKeyEntry.getCertificateChain();
    }

    //Postconditions
    assert certificateChain != null;
    assert certificateChain.length >= 2;

    return certificateChain;
  }

  /** Returns the X.509 certificate.
   *
   * @return the X.509 certificate
   */
  public X509Certificate getX509Certificate() {
    return getCertificateChain()[0];
  }

  /** Returns the certificate path.
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

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "[X509 certificate information for ...\n" + getX509Certificate().toString() + "]";
  }

  /** Returns whether the X509 certificate can be used for digital signing.
   *
   * @return the X509 certificate can be used for digital signing
   */
  public boolean isDigitialSigniatureCertificate() {
    return X509Utils.hasKeyUsage(getCertificateChain()[0], KeyUsage.digitalSignature);

  }
}
