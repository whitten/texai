/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.kb.persistence.benchmark;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.kb.persistence.KBAccess;

/**
 *
 * @author reed
 */
public class PerformanceBenchmarkTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PerformanceBenchmarkTest.class);

  public PerformanceBenchmarkTest() {
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
   * Test of main method, of class PerformanceBenchmark.
   */
  @Test
  public void testMain() {
    LOGGER.info("local Sesame repository benchmark");
    Logger.getLogger(KBAccess.class).setLevel(Level.WARN);
    final PerformanceBenchmark performanceBenchmark = new PerformanceBenchmark();
    performanceBenchmark.setNbrRDFTestEntitiesToCreate(1000);
    performanceBenchmark.initialization();
    performanceBenchmark.createLinkedRDFTestEntities();
    performanceBenchmark.queryInstanceURIs();
    performanceBenchmark.setNbrRDFTestEntitiesToRead(2000);
    performanceBenchmark.readLinkedRDFTestEntities();
    performanceBenchmark.finalization();
  }
}
