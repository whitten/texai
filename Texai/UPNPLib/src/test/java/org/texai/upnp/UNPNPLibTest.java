/*
 * UNPNPLibTest.java
 *
 * Created on Jan 12, 2010, 5:50:41 AM
 *
 * Description: Demonstrates and tests the UPNPLib Universal Plug and Play library for NAT traversal.
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
package org.texai.upnp;

import java.io.IOException;
import java.net.InetAddress;
import net.sbbi.upnp.Discovery;
import net.sbbi.upnp.devices.UPNPRootDevice;
import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sbbi.upnp.messages.ActionResponse;
import net.sbbi.upnp.messages.UPNPResponseException;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.util.NetworkUtils;
import static org.junit.Assert.*;

/** Demonstrates and tests the UPNPLib Universal Plug and Play library for NAT traversal.
 *
 * @author reed
 */
public class UNPNPLibTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(UNPNPLibTest.class);
  /** the discovery timeout of 5 seconds */
  private static final int DISCOVERY_TIMEOUT = 3000;
  /** the test port */
  private static final int TEST_PORT = 9090;

  public UNPNPLibTest() {
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
   * Test of UPNPLib library
   */
  @Test
  public void testDiscovery() {
    LOGGER.info("discovery");
    try {
      final UPNPRootDevice[] allDevices = Discovery.discover(
              DISCOVERY_TIMEOUT,
              Discovery.DEFAULT_TTL,
              Discovery.DEFAULT_MX,
              Discovery.DEFAULT_SEARCH);
      if (allDevices != null) {
        for (int i = 0; i < allDevices.length; i++) {
          final UPNPRootDevice upnRootDevice = allDevices[i];
          LOGGER.info("Found device");
          LOGGER.info("  device type: " + upnRootDevice.getDeviceType());
          LOGGER.info("  friendly name: " + upnRootDevice.getFriendlyName());
          LOGGER.info("  manufacturer: " + upnRootDevice.getManufacturer());
          LOGGER.info("  model description: " + upnRootDevice.getModelDescription());
          LOGGER.info("  model name: " + upnRootDevice.getModelName());
          LOGGER.info("  model number: " + upnRootDevice.getModelNumber());
          LOGGER.info("  model URL: " + upnRootDevice.getModelURL());
          LOGGER.info("  serial number: " + upnRootDevice.getSerialNumber());
          LOGGER.info("  vendor firmware: " + upnRootDevice.getVendorFirmware());
        }
      }
      try {
        // wait for discovery listener thread to finish
        Thread.sleep(1000);
        //Thread.sleep(1000000);
      } catch (InterruptedException ex) {
        fail(ex.getMessage());
      }

      final String st = "urn:schemas-upnp-org:device:InternetGatewayDevice:1";
      final UPNPRootDevice[] igdDevices = Discovery.discover(st);
      if (igdDevices != null) {
        for (int i = 0; i < igdDevices.length; i++) {
          final UPNPRootDevice upnRootDevice = igdDevices[i];
          LOGGER.info("Found IGD device");
          LOGGER.info("  device type: " + upnRootDevice.getDeviceType());
          LOGGER.info("  friendly name: " + upnRootDevice.getFriendlyName());
          LOGGER.info("  manufacturer: " + upnRootDevice.getManufacturer());
          LOGGER.info("  model description: " + upnRootDevice.getModelDescription());
          LOGGER.info("  model name: " + upnRootDevice.getModelName());
          LOGGER.info("  model number: " + upnRootDevice.getModelNumber());
          LOGGER.info("  model URL: " + upnRootDevice.getModelURL());
          LOGGER.info("  serial number: " + upnRootDevice.getSerialNumber());
          LOGGER.info("  vendor firmware: " + upnRootDevice.getVendorFirmware());
        }
      }

    } catch (IOException ex) {
      fail(ex.getMessage());
    }
    try {
      // wait for discovery listener thread to finish
      Thread.sleep(1000);
      //Thread.sleep(1000000);
    } catch (InterruptedException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of UPNPLib library
   */
  @Test
  public void testNATMapping() {
    LOGGER.info("nat mapping");
    try {
      final InternetGatewayDevice[] IGDs = InternetGatewayDevice.getDevices(DISCOVERY_TIMEOUT);
      if (IGDs != null) {
        // let's use the the first device found
        final InternetGatewayDevice internetGatewayDevice = IGDs[0];
        LOGGER.info("Found device " + internetGatewayDevice.getIGDRootDevice().getModelDescription());
        // now let's open the port
        final InetAddress localHostAddress = NetworkUtils.getLocalHostAddress();
        LOGGER.info("local host address: " + localHostAddress.getHostAddress());
        // we assume that localHostIP is something else than 127.0.0.1
        final boolean isMapped = internetGatewayDevice.addPortMapping(
                "test nat mapping", // description
                null, // remote host
                TEST_PORT, // internal port
                TEST_PORT, // external port
                localHostAddress.getHostAddress(),
                0, // lease duration in seconds, 0 for an infinite time
                "TCP");  // protocol
        if (isMapped) {
          LOGGER.info("Port " + TEST_PORT + " mapped to " + localHostAddress.getHostAddress());

          final ActionResponse actionResponse = internetGatewayDevice.getSpecificPortMappingEntry(
                  null,   // remoteHost
                  TEST_PORT, // external port
                  "TCP");  // protocol
          LOGGER.info("mapping info:\n" + actionResponse);
          
          // and now close it
          final boolean isUnmapped = internetGatewayDevice.deletePortMapping(
                  null,  // remoteHost
                  TEST_PORT,  // external port
                  "TCP");  // protocol
          if (isUnmapped) {
            LOGGER.info("Port " + TEST_PORT + " unmapped");
          }
        }
      }
    } catch (IOException ex) {
      fail(ex.getMessage());
    } catch (UPNPResponseException respEx) {
      fail(respEx.getMessage());
    }
    try {
      // wait for discovery listener thread to finish
      Thread.sleep(1000);
    } catch (InterruptedException ex) {
      fail(ex.getMessage());
    }
  }
}
