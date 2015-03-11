package org.texai.ahcsSupport;

import org.texai.ahcsSupport.seed.SeedNodeInfo;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.texai.util.NetworkUtils;

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
    assertEquals("TestContainer.ContainerOperationAgent.ContainerSingletonConfigurationRole", instance.getQualifiedName());
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
   * Test of hashCode method, of class SeedNodeInfo.
   */
  @Test
  public void testHashCode() {
    LOGGER.info("hashCode");
    assertEquals(-71049763, instance.hashCode());
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
    assertEquals("[Seed TestContainer.ContainerOperationAgent.ContainerSingletonConfigurationRole gandalf:5048]", instance.toString());
  }

  private static SeedNodeInfo makeSeedNodesInfo() {
    final String qualifiedName = "TestContainer.ContainerOperationAgent.ContainerSingletonConfigurationRole";
    final String hostName= "gandalf";
    final int port = NetworkUtils.TEXAI_MAINNET_PORT;

    return new SeedNodeInfo(
            qualifiedName,
            hostName,
            port);
  }
}
