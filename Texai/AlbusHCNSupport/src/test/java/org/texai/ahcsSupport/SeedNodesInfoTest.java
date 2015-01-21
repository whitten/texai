package org.texai.ahcsSupport;

import org.texai.ahcsSupport.seed.SeedNodeInfo;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.texai.util.NetworkUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509Utils;

/**
 *
 * @author reed
 */
public class SeedNodesInfoTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(SeedNodesInfoTest.class);
  // the test instance
  private static final SeedNodeInfo instance = makeSeedNodesInfo();

  public SeedNodesInfoTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of getQualifiedName method, of class SeedNodeInfo.
   */
  @Test
  public void testGetQualifiedName() {
    LOGGER.info("getQualifiedName");
    assertEquals("TestContainer.SingletonConfigurationAgent.SingletonConfigurationRole", instance.getQualifiedName());
  }

  /**
   * Test of getHostName method, of class SeedNodeInfo.
   */
  @Test
  public void testGetHostName() {
    LOGGER.info("getHostName");
    assertEquals("gandalf", instance.getHostName());
  }

  /**
   * Test of getPort method, of class SeedNodeInfo.
   */
  @Test
  public void testGetPort() {
    LOGGER.info("getPort");
    assertEquals(5048, instance.getPort());
  }

  /**
   * Test of getX509Certificate method, of class SeedNodeInfo.
   */
  @Test
  public void testGetX509Certificate() {
    LOGGER.info("getX509Certificate");
    assertTrue(instance.getX509Certificate().getSubjectDN().toString().startsWith("CN=texai.org, DC=TestContainer.SingletonConfigurationAgent.SingletonConfigurationRole, UID="));
  }

  /**
   * Test of hashCode method, of class SeedNodeInfo.
   */
  @Test
  public void testHashCode() {
    LOGGER.info("hashCode");
    assertEquals(445624413, instance.hashCode());
  }

  /**
   * Test of equals method, of class SeedNodeInfo.
   */
  @Test
  public void testEquals() {
    LOGGER.info("equals");
    assertEquals(instance, makeSeedNodesInfo());
    assertFalse("abc".equals(instance));
  }

  /**
   * Test of toString method, of class SeedNodeInfo.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    assertEquals("[Seed TestContainer.SingletonConfigurationAgent.SingletonConfigurationRole gandalf:5048]", instance.toString());
  }

  private static SeedNodeInfo makeSeedNodesInfo() {
    final String qualifiedName = "TestContainer.SingletonConfigurationAgent.SingletonConfigurationRole";
    final String hostName= "gandalf";
    final int port = NetworkUtils.TEXAI_PORT;
    final X509Certificate x509Certificate;
    final KeyPair keyPair;
    try {
      keyPair = X509Utils.generateRSAKeyPair3072();
      x509Certificate = X509Utils.generateSelfSignedEndEntityX509Certificate(
              keyPair,
              UUID.randomUUID(), // uid
              qualifiedName); // domainComponent
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | CertificateParsingException | CertificateEncodingException | SignatureException | InvalidKeyException | IOException ex) {
      throw new TexaiException(ex);
    }

    return new SeedNodeInfo(
            qualifiedName,
            hostName,
            port,
            x509Certificate);
  }
}
