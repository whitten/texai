package org.texai.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;
import org.apache.log4j.Logger;

/**
 * TexaiTrustManager.java
 *
 * Description: Provides a trust manager for Texai self-signed certificates.
 *
 * Copyright (C) Oct 15, 2014, Stephen L. Reed, Texai.org.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation,
 * Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
public class TexaiTrustManager implements X509TrustManager {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(X509TrustManager.class);

  /**
   * Creates a new instance of TexaiTrustManager.
   */
  public TexaiTrustManager() {
  }

  /**
   * Given the partial or complete certificate chain provided by the peer, build a certificate path to a trusted root and return if it can
   * be validated and is trusted for client SSL authentication based on the authentication type.
   *
   * The authentication type is the key exchange algorithm portion of the cipher suites represented as a String, such as "RSA", "DHE_DSS".
   * Note: for some exportable cipher suites, the key exchange algorithm is determined at run time during the handshake. For instance, for
   * TLS_RSA_EXPORT_WITH_RC4_40_MD5, the authType should be RSA_EXPORT when an ephemeral RSA key is used for the key exchange, and RSA when
   * the key from the server certificate is used. Checking is case-sensitive.
   *
   * @param certificateChain the X.509 certificate chain, which is expected to be a single certificate
   * @param authType the key exchange algorithm portion of the cipher suites represented as a String, e.g. "RSA"
   *
   * @throws java.security.cert.CertificateException
   */
  @Override
  public void checkClientTrusted(
          final X509Certificate[] certificateChain,
          final String authType) throws CertificateException {
    //Preconditions
    assert certificateChain != null : "certificateChain must not be null";

    LOGGER.info("server checking the client's X.509 certificate ...");
    if (certificateChain.length != 1) {
      throw new CertificateException("Expected client certificate chain length 1, received length " + certificateChain.length);
    }
    if (!authType.equals("RSA")) {
      throw new CertificateException("Expected client authType RSA, received " + authType);
    }
    //TODO validate the subject info
  }

  /**
   * Given the partial or complete certificate chain provided by the peer, build a certificate path to a trusted root and return if it can
   * be validated and is trusted for server SSL authentication based on the authentication type.
   *
   * The authentication type is the key exchange algorithm portion of the cipher suites represented as a String, such as "RSA", "DHE_DSS".
   * Note: for some exportable cipher suites, the key exchange algorithm is determined at run time during the handshake. For instance, for
   * TLS_RSA_EXPORT_WITH_RC4_40_MD5, the authType should be RSA_EXPORT when an ephemeral RSA key is used for the key exchange, and RSA when
   * the key from the server certificate is used. Checking is case-sensitive.
   *
   * @param certificateChain the X.509 certificate chain, which is expected to be a single certificate
   * @param authType the key exchange algorithm portion of the cipher suites represented as a String, e.g. "RSA"
   *
   * @throws java.security.cert.CertificateException
   */
  @Override
  public void checkServerTrusted(
          final X509Certificate[] certificateChain,
          final String authType) throws CertificateException {
    //Preconditions
    assert certificateChain != null : "certificateChain must not be null";

    LOGGER.info("client checking the server's X.509 certificate ...");
    if (certificateChain.length != 1) {
      throw new CertificateException("Expected client certificate chain length 1, received length " + certificateChain.length);
    }
    if (!authType.equals("RSA")) {
      throw new CertificateException("Expected server authType RSA, received " + authType);
    }
    //TODO validate the subject info
  }

  /**
   * Return an array of certificate authority certificates which are trusted for authenticating peers.
   *
   * @return an array of certificate authority certificates which are trusted for authenticating peers
   */
  @Override
  public X509Certificate[] getAcceptedIssuers() {
    LOGGER.info("getting CA certificates ...");
    final X509Certificate[] acceptedIssuers = {};
    return acceptedIssuers;
  }

}
