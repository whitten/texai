/*
 * UUIDUtilsTest.java
 *
 * Created on Jun 30, 2008, 8:18:27 PM
 *
 * Description: .
 *
 * Copyright (C) Apr 20, 2010 reed.
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
