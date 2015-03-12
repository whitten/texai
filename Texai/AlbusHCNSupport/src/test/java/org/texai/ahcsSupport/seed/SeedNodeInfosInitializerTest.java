/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.ahcsSupport.seed;

import java.util.Set;
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
public class SeedNodeInfosInitializerTest {
  // the logger
  private static final Logger LOGGER = Logger.getLogger(SeedNodeInfosInitializerTest.class);

  public SeedNodeInfosInitializerTest() {
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
   * Test of process method, of class SeedNodeInfosInitializer.
   */
  @Test
  public void testProcess() {
    LOGGER.info("process");
    SeedNodeInfosInitializer instance = new SeedNodeInfosInitializer("testnet");
    Set<SeedNodeInfo> result = instance.process();
    assertEquals("[[Seed Mint.ContainerOperationAgent.ContainerSingletonConfigurationRole Mint:45048]]", result.toString());
  }

}
