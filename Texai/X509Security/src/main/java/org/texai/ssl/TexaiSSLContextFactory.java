/*
 * TexaiSSLContextFactory.java
 *
 * Created on Feb 2, 2010, 9:01:29 AM
 *
 * Description: Provides a SSL context factory.
 *
 * Copyright (C) Feb 2, 2010 reed.
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
package org.texai.ssl;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.apache.log4j.Logger;
import org.texai.util.TexaiException;
import org.texai.x509.X509SecurityInfo;
import org.texai.x509.X509Utils;

/** Provides a SSL context factory.
 *
 * @author reed
 */
public final class TexaiSSLContextFactory {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(TexaiSSLContextFactory.class);
  // the enabled SSL cipher suites
  private static String[] enabledCipherSuites;
  // the enabled SSL cipher suites lock
  private static final Object ENABLED_CIPHER_SUITES_LOCK = new Object();
  // the iOS incompatible cipher suites
  private static final List<String> iOSIncompatibleCipherSuites = new ArrayList<>();

  /** Prevents this utility class from being instantiated. */
  private TexaiSSLContextFactory() {
  }

  /** Gets the Secure Sockets Layer context.
   *
   * @param x509SecurityInfo the X.509 security information
   * @return the SSL context
   */
  public static SSLContext getSSLContext(final X509SecurityInfo x509SecurityInfo) {
    //Preconditions
    assert x509SecurityInfo != null : "x509SecurityInfo must not be null";

    // create key manager factory and SSL context

    try {
      final SSLContext sslContext = SSLContext.getInstance("TLS");
      final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
      trustManagerFactory.init(x509SecurityInfo.getTrustStore());
      final X509TrustManager x509TrustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
      final X509TrustManager[] x509TrustManagers = new X509TrustManager[]{x509TrustManager};
      sslContext.init(
              x509SecurityInfo.getKeyManagers(), // the sources of authentication keys
              x509TrustManagers, // the sources of peer authentication trust decisions
              X509Utils.getSecureRandom());  // the source of randomness for this generator or null
      return sslContext;
    } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Configures the SSL engine for client or for the server. Arranges the enabled ciphers to favor the
   * most secure over the less secure, and omits the least secure ciphers.  Requires that the SSL server
   * authenticate the client.
   *
   * @param sslEngine the SSL engine
   * @param useClientMode the indicator whether the SSL engine is operating in client mode
   * @param needClientAuth the indicator whether the server authenticates the client's SSL certificate
   */
  public static synchronized void configureSSLEngine(
          final SSLEngine sslEngine,
          final boolean useClientMode,
          final boolean needClientAuth) {
    //Preconditions
    assert sslEngine != null : "sslEngine must not be null";

    if (useClientMode) {
      LOGGER.info("configuring SSL engine for the client side of the connection");
      sslEngine.setUseClientMode(true);
      sslEngine.setNeedClientAuth(false);
    } else {
      if (needClientAuth) {
        LOGGER.info("configuring SSL engine for the server side of the connection with required client authorization");
      } else {
        LOGGER.info("configuring SSL engine for the server side of the connection without required client authorization");
      }
      sslEngine.setUseClientMode(false);
      sslEngine.setNeedClientAuth(needClientAuth);
    }
    synchronized (ENABLED_CIPHER_SUITES_LOCK) {
      if (enabledCipherSuites == null) {
        iOSIncompatibleCipherSuites.add("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384");
        iOSIncompatibleCipherSuites.add("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256");
        iOSIncompatibleCipherSuites.add("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA");
        iOSIncompatibleCipherSuites.add("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");

        // TLS_ECDHE_RSA_WITH_RC4_128_SHA is the negotiated cipher suite for iOS

        // select and arrange the highest security cipher suites and cache the result
        final String[] supportedCipherSuites = sslEngine.getSupportedCipherSuites();
        final List<String> enabledCipherSuitesList = new ArrayList<>(supportedCipherSuites.length);
        // The first pass selects 256 bit ciphers available with the Java Cryptography Extension (JCE)
        // Unlimited Strength Jurisdiction Policy Files, downloaded and installed from http://java.sun.com/javase/downloads/index.jsp .
        for (final String supportedCipherSuite : supportedCipherSuites) {
          if (supportedCipherSuite.contains("_256_") && !supportedCipherSuite.contains("_anon_")) {
            enabledCipherSuitesList.add(supportedCipherSuite);
          }
        }
        // The second pass selects 128 bit ciphers that use SHA hashing - its more secure than MD5 but slower.
        for (final String supportedCipherSuite : supportedCipherSuites) {
          if (supportedCipherSuite.contains("_128_") && !supportedCipherSuite.endsWith("_MD5") && !supportedCipherSuite.contains("_anon_")) {
            enabledCipherSuitesList.add(supportedCipherSuite);
          }
        }
        // The third pass selects 128 bit ciphers that use MD5 hashing.
        for (final String supportedCipherSuite : supportedCipherSuites) {
          if (supportedCipherSuite.contains("_128_") && supportedCipherSuite.endsWith("_MD5") && !supportedCipherSuite.contains("_anon_")) {
            enabledCipherSuitesList.add(supportedCipherSuite);
          }
        }
        // The fourth pass removes the iOS incompatible cipher suites
        enabledCipherSuitesList.removeAll(iOSIncompatibleCipherSuites);

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("enabledCipherSuites: " + enabledCipherSuitesList);
        }
        final int enabledCipherSuitesList_size = enabledCipherSuitesList.size();
        enabledCipherSuites = new String[enabledCipherSuitesList_size];
        for (int i = 0; i < enabledCipherSuitesList_size; i++) {
          enabledCipherSuites[i] = enabledCipherSuitesList.get(i);
        }
      }
      sslEngine.setEnabledCipherSuites(enabledCipherSuites);
    }
  }
}
