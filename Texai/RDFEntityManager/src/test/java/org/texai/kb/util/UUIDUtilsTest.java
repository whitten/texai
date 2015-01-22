/*
 * UUIDUtilsTest.java
 *
 * Created on Jun 30, 2008, 8:18:27 PM
 *
 * Description: .
 *
 * Copyright (C) Apr 20, 2010 reed.
 */
package org.texai.kb.util;

import java.util.UUID;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openrdf.model.impl.URIImpl;

/**
 *
 * @author reed
 */
public class UUIDUtilsTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(UUIDUtilsTest.class);

  public UUIDUtilsTest() {
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
   * Test of uriToUUID method, of class UUIDUtils.
   */
  @Test
  public void testUriToUUID() {
    LOGGER.info("uriToUUID");
    UUID result = UUIDUtils.uriToUUID(new URIImpl("http://texai.org/texai/org.texai.ahcs.domainEntity.Role_d828a5c6-c434-4739-bfd3-66b99958ba0a"));
    assertEquals("d828a5c6-c434-4739-bfd3-66b99958ba0a", result.toString());
    result = UUIDUtils.uriToUUID(new URIImpl("http://texai.org/texai/d828a5c6-c434-4739-bfd3-66b99958ba0a"));
    assertEquals("d828a5c6-c434-4739-bfd3-66b99958ba0a", result.toString());
  }
}
