/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.deployment;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
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
public class CreateSoftwareDeploymentManifestTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(CreateSoftwareDeploymentManifestTest.class);

  public CreateSoftwareDeploymentManifestTest() {
  }

  @BeforeClass
  public static void setUpClass() {
    final File testManifestsDirectory = new File("data/test-deployment-manifests");
    assertTrue(testManifestsDirectory.exists());
    final File[] files = testManifestsDirectory.listFiles();
    for (final File file : files) {
      if (!file.isHidden()) {
        LOGGER.info("deleting previous test file: " + file);
        file.delete();
      }
    }

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
   * Test of advanceIterator method, of class CreateSoftwareDeploymentManifest.
   */
  @Test
  public void testAdvanceIterator() {
    LOGGER.info("advanceIterator");
    final List<File> files = new ArrayList<>();
    files.add(new File("a"));
    files.add(new File("b"));
    final Iterator<File> files_iter = files.iterator();
    File file = CreateSoftwareDeploymentManifest.advanceIterator(files_iter);
    assertEquals("a", file.toString());
    file = CreateSoftwareDeploymentManifest.advanceIterator(files_iter);
    assertEquals("b", file.toString());
    file = CreateSoftwareDeploymentManifest.advanceIterator(files_iter);
    assertNull(file);
    assertTrue(!files_iter.hasNext());
  }

  /**
   * Test of isIgnored method, of class CreateSoftwareDeploymentManifest.
   */
  @Test
  public void testIsIgnored() {
    LOGGER.info("isIgnored");

    CreateSoftwareDeploymentManifest createSoftwareDeploymentManifest = new CreateSoftwareDeploymentManifest(
            "data/test-deployment-dir-old", // oldDirectoryPath
            "data/test-deployment-dir-new", // newDirectoryPath
            "data/test-deployment-manifests"); // manifestDirectoryPath
    assertTrue(createSoftwareDeploymentManifest.isIgnored(new File(".aicoin")));
    assertTrue(createSoftwareDeploymentManifest.isIgnored(new File("keystore.uber")));
    assertFalse(createSoftwareDeploymentManifest.isIgnored(new File("nodes.xml")));
  }

  /**
   * Test of main method, of class CreateSoftwareDeploymentManifest.
   */
  @Test
  public void testMain() {
    LOGGER.info("main");
    String[] args = {
      "data/test-deployment-dir-old",
      "data/test-deployment-dir-new",
      "data/test-deployment-manifests"
    };
    CreateSoftwareDeploymentManifest.main(args);
  }

}
