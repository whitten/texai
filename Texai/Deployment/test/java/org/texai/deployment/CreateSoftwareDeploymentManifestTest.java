/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.deployment;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author reed
 */
public class CreateSoftwareDeploymentManifestTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(CreateSoftwareDeploymentManifestTest.class);

  public CreateSoftwareDeploymentManifestTest() {
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
   * Test of main method, of class CreateSoftwareDeploymentManifest.
   */
  @Test
  public void testMain() {
    LOGGER.info("main");
    final String[] args = {
      "data/test-deployment-dir-old",
      "data/test-deployment-dir-new",
      "data/test-deployment-manifests"
    };
    CreateSoftwareDeploymentManifest.main(args);
  }

}
