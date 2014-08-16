/*
 * CipherSuiteNegotiationSimulation.java
 *
 * Created on Jul 9, 2012, 4:00:15 PM
 *
 * Description: Performs a simulation of the SSL/TLS cipher suite negotiation that occurs when an iOS client
 * connects with the Texai https server.
 *
 * Copyright (C) Jul 9, 2012, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.ssl;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.util.StringUtils;

/** Performs a simulation of the SSL/TLS cipher suite negotiation that occurs when an iOS client
 * connects with the Texai https server.
 *
 * @author reed
 */
@NotThreadSafe
public class CipherSuiteNegotiationSimulation {

  private static final Logger LOGGER = Logger.getLogger(CipherSuiteNegotiationSimulation.class);
  /** the iOS cipher suites */
  private static final String[] IOS_CIPHER_SUITES = {
    "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
    "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
    "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
    "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384",
    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
    "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384",
    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",
    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
    "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
    "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
    "TLS_ECDH_RSA_WITH_RC4_128_SHA",
    "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
    "TLS_RSA_WITH_AES_256_CBC_SHA256",
    "TLS_RSA_WITH_AES_128_CBC_SHA256",
    "TLS_RSA_WITH_AES_128_CBC_SHA",
    "SSL_RSA_WITH_RC4_128_SHA",
    "SSL_RSA_WITH_RC4_128_MD5",
    "TLS_RSA_WITH_AES_256_CBC_SHA",
    "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
    "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA"
  };
  /** the Java cipher suites */
  private static final String[] JAVA_CIPHER_SUITES = {
    "TLS_ECDH_ECDSA_WITH_NULL_SHA",
    "TLS_DH_anon_WITH_AES_128_CBC_SHA256",
    "TLS_ECDH_anon_WITH_RC4_128_SHA",
    "TLS_DH_anon_WITH_AES_128_CBC_SHA",
    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
    "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
    "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
    "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
    "TLS_KRB5_WITH_3DES_EDE_CBC_SHA",
    "SSL_RSA_WITH_RC4_128_SHA",
    "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
    "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
    "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
    "TLS_KRB5_EXPORT_WITH_RC4_40_SHA",
    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",
    "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
    "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
    "TLS_ECDH_anon_WITH_NULL_SHA",
    "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
    "TLS_ECDH_anon_WITH_AES_256_CBC_SHA",
    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
    "TLS_DH_anon_WITH_AES_256_CBC_SHA",
    "TLS_RSA_WITH_NULL_SHA256",
    "TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA",
    "TLS_KRB5_WITH_RC4_128_MD5",
    "SSL_RSA_WITH_DES_CBC_SHA",
    "TLS_ECDHE_ECDSA_WITH_NULL_SHA",
    "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
    "TLS_ECDH_RSA_WITH_RC4_128_SHA",
    "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
    "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
    "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384",
    "TLS_DH_anon_WITH_AES_256_CBC_SHA256",
    "TLS_KRB5_WITH_3DES_EDE_CBC_MD5",
    "TLS_KRB5_WITH_RC4_128_SHA",
    "SSL_DH_anon_WITH_DES_CBC_SHA",
    "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
    "TLS_ECDH_RSA_WITH_NULL_SHA",
    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
    "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
    "TLS_KRB5_WITH_DES_CBC_MD5",
    "TLS_KRB5_EXPORT_WITH_RC4_40_MD5",
    "TLS_RSA_WITH_AES_256_CBC_SHA256",
    "TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5",
    "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
    "TLS_ECDH_anon_WITH_AES_128_CBC_SHA",
    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
    "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5",
    "SSL_DHE_DSS_WITH_DES_CBC_SHA",
    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
    "TLS_KRB5_WITH_DES_CBC_SHA",
    "SSL_RSA_WITH_NULL_MD5",
    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
    "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
    "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
    "TLS_RSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384",
    "SSL_DHE_RSA_WITH_DES_CBC_SHA",
    "TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA",
    "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA",
    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
    "SSL_RSA_WITH_NULL_SHA",
    "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
    "TLS_ECDHE_RSA_WITH_NULL_SHA",
    "TLS_RSA_WITH_AES_256_CBC_SHA",
    "SSL_DH_anon_WITH_RC4_128_MD5",
    "TLS_RSA_WITH_AES_128_CBC_SHA256",
    "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
    "SSL_RSA_WITH_RC4_128_MD5",
    "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
    "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
    "SSL_RSA_WITH_3DES_EDE_CBC_SHA"
  };

  /** Constructs a new CipherSuiteNegotiationSimulation instance. */
  public CipherSuiteNegotiationSimulation() {
  }

  /** Simulates an SSL cipher suite negotiation between an iOS client and the Texai server, in order to help find
   * compatible cipher suites for iOS clients which cannot be modified with respect to candidate cipher suites.
   */
  private void simulate() {
    for (final String iOSCipherSuite : IOS_CIPHER_SUITES) {
      if (javaCipherSuitesContain(iOSCipherSuite)) {
        LOGGER.info("matched: " + iOSCipherSuite);
      } else {
        LOGGER.info("         " + iOSCipherSuite);
      }
    }
  }

  /** Returns whether the java cipher suites contain the given iOS cipher suite.
   *
   * @param iOSCipherSuite the given iOS cipher suite
   * @return whether the java cipher suites contain the given iOS cipher suite
   */
  private boolean javaCipherSuitesContain(final String iOSCipherSuite) {
    //Preconditons
    assert StringUtils.isNonEmptyString(iOSCipherSuite) : "iOSCipherSuite must be a non-empty string";

    for (final String javaCipherSuite : JAVA_CIPHER_SUITES) {
      if (iOSCipherSuite.equals(javaCipherSuite)) {
        return true;
      }
    }
    return false;
  }

  /** Executes this application.
   *
   * @param args the command line arguments - unused
   */
  public static void main(final String[] args) {
    final CipherSuiteNegotiationSimulation cipherSuiteNegotiationSimulation = new CipherSuiteNegotiationSimulation();
    cipherSuiteNegotiationSimulation.simulate();
  }
}
