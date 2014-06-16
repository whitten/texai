/*
 * NetworkUtilsTest.java
 *
 * Created on Jun 30, 2008, 9:20:22 AM
 *
 * Description: .
 *
 * Copyright (C) Jan 12, 2010 reed.
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
package org.texai.util;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public class NetworkUtilsTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(NetworkUtilsTest.class);

  public NetworkUtilsTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of getLocalHostAddress method, of class NetworkUtils.
   */
  @Test
  public void testGetLocalHostAddress() {
    LOGGER.info("getLocalHostAddress");
    InetAddress result = NetworkUtils.getLocalHostAddress();
    LOGGER.info("local host address: " + result);
    assertNotNull(result);
  }

  /**
   * Test of isPrivateNetworkAddress method, of class NetworkUtils.
   */
  @Test
  public void testIsPrivateNetworkAddress() {
    LOGGER.info("isPrivateNetworkAddress");
    InetAddress inetAddress = null;
    try {
      inetAddress = InetAddress.getByName("www.google.com");
      LOGGER.info("google.com: " + inetAddress.getHostAddress());
      assertFalse(NetworkUtils.isPrivateNetworkAddress(inetAddress));
      inetAddress = InetAddress.getLocalHost();
      if (inetAddress.isLoopbackAddress()) {
        LOGGER.info("local host: " + inetAddress.getHostAddress() + " is a loopback address");
      } else if (NetworkUtils.isPrivateNetworkAddress(inetAddress)) {
        LOGGER.info("local host: " + inetAddress.getHostAddress() + " is a private Internet address");
      } else {
        LOGGER.info("local host: " + inetAddress.getHostAddress() + " is not private Internet address");
      }
    } catch (UnknownHostException ex) {
      fail(ex.getMessage());
    }
    assertNotNull(inetAddress);
  }

  /**
   * Test of getMACAddress method, of class NetworkUtils.
   */
  @Test
  public void testGetMACAddress() {
    LOGGER.info("getMACAddress");
    final List<Byte> macAddress = NetworkUtils.getMACAddress();
    assertTrue(!macAddress.isEmpty());
  }

  /**
   * Test of getMACAddressString method, of class NetworkUtils.
   */
  @Test
  public void testGetMACAddressString() {
    LOGGER.info("getMACAddressString");
    final String macAddressString = NetworkUtils.getMACAddressString();
    LOGGER.info("MAC address: " + macAddressString);
    assertTrue(!macAddressString.isEmpty());
  }

  /**
   * Test of getDynamicServerPort method, of class NetworkUtils.
   */
  @Test
  public void testGetServerPort() {
    LOGGER.info("getServerPort");
    int serverPort = NetworkUtils.getDynamicServerPort();
    LOGGER.info("serverPort: " + serverPort);
    assertTrue(serverPort >= NetworkUtils.LOWER_PORT_BOUND);
    assertTrue(serverPort <= NetworkUtils.UPPER_PORT_BOUND);
    assertTrue((new File(NetworkUtils.SERVER_PORT_PATH)).exists());
    assertEquals(serverPort, NetworkUtils.getDynamicServerPort());
    serverPort = NetworkUtils.getRandomDynamicServerPort();
    assertTrue(serverPort >= NetworkUtils.LOWER_PORT_BOUND);
    assertTrue(serverPort <= NetworkUtils.UPPER_PORT_BOUND);
  }

  /**
   * Test of isCloudService method, of class NetworkUtils.
   */
  @Test
  public void testIsCloudService() {
    LOGGER.info("isCloudService");
    assertTrue(NetworkUtils.isCloudService());
  }

  /**
   * Test of isDistributedTexaiInstance method, of class NetworkUtils.
   */
  @Test
  public void testIsDistributedTexaiInstance() {
    LOGGER.info("isDistributedTexaiInstance");
    assertTrue(!NetworkUtils.isDistributedTexaiInstance());
  }

  /**
   * Test of isHostAvailable method, of class NetworkUtils.
   */
  @Test
  public void testIsHostAvailable() {
    LOGGER.info("isHostAvailable");
    String host = "texai.org";
    int port = 80;
    boolean isHostAvailable = NetworkUtils.isHostAvailable(host, port);
    if (isHostAvailable) {
      LOGGER.info(host + " is available for connections on port " + port);
    } else {
      LOGGER.info(host + " is not available for connections on port " + port);
    }

    host = "texai.dyndns.org";
    port = 443;
    isHostAvailable = NetworkUtils.isHostAvailable(host, port);
    if (isHostAvailable) {
      LOGGER.info(host + " is available for connections on port " + port);
    } else {
      LOGGER.info(host + " is not available for connections on port " + port);
    }
  }

  /**
   * Test of makeInetSocketAddress method, of class NetworkUtils.
   */
  @Test
  public void testMakeInetSocketAddress() {
    LOGGER.info("makeInetSocketAddress");
    final String socketAddressString = NetworkUtils.makeInetSocketAddress("http://mccarthy:61355/").toString();
    assertTrue(socketAddressString.startsWith("mccarthy"));
    assertTrue(socketAddressString.endsWith(":61355"));
  }

  /**
   * Test of getHostName method, of class NetworkUtils.
   */
  @Test
  public void testGetHostName() {
    LOGGER.info("getHostName");
    final String hostName = NetworkUtils.getHostName();
    assertNotNull(hostName);
    LOGGER.info("this host name is '" + hostName + "'");
    assertTrue(!hostName.isEmpty());
  }

}
