/*
 * XMLUtilsTest.java
 *
 * Created on Jun 30, 2008, 8:48:49 AM
 *
 * Description: .
 *
 * Copyright (C) Apr 12, 2012 reed.
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

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.w3c.dom.Document;

/**
 *
 * @author reed
 */
public class XMLUtilsTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PatternsTest.class);
  /** the test XML string */
  private static final String XML_STRING =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          + "<feed>\n"
          + "<entry xmlns=\"http://www.w3.org/2005/Atom\"><id>tag:search.twitter.com,2005:190215834591956992</id><published>2012-04-11T23:12:43Z</published><link type=\"text/html\" href=\"http://twitter.com/David_Wilen/statuses/190215834591956992\" rel=\"alternate\"/><title>I'm at Elvis Restaurant (Baltimore, MD) http://t.co/ioDMMZ34</title><content type=\"html\">I'm at &lt;em&gt;Elvis&lt;/em&gt; Restaurant (Baltimore, MD) &lt;a href=\"http://t.co/ioDMMZ34\"&gt;http://t.co/ioDMMZ34&lt;/a&gt;</content><updated>2012-04-11T23:12:43Z</updated><link type=\"image/png\" href=\"http://a0.twimg.com/profile_images/1781430382/image_normal.jpg\" rel=\"image\"/><twitter:geo xmlns:twitter=\"http://api.twitter.com/\"/><twitter:metadata xmlns:twitter=\"http://api.twitter.com/\"><twitter:result_type>recent</twitter:result_type></twitter:metadata><twitter:source xmlns:twitter=\"http://api.twitter.com/\">&lt;a href=\"http://foursquare.com\" rel=\"nofollow\"&gt;foursquare&lt;/a&gt;</twitter:source><twitter:lang xmlns:twitter=\"http://api.twitter.com/\">en</twitter:lang><author><name>David_Wilen (David Wilen)</name><uri>http://twitter.com/David_Wilen</uri></author></entry>\n"
          + "</feed>";

  public XMLUtilsTest() {
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
   * Test of loadXMLFromString method, of class XMLUtils.
   */
  @Test
  public void testLoadXMLFromString() {
    LOGGER.info("loadXMLFromString");
    Document result = XMLUtils.loadXMLFromString(XML_STRING);
    assertNotNull(result);
    assertEquals("UTF-8", result.getXmlEncoding());
    assertNull(result.getBaseURI());
    assertNull(result.getDoctype());
    assertNull(result.getDocumentURI());
    assertEquals("1.0", result.getXmlVersion());
  }

  /**
   * Test of prettyPrintWithDOM3LS method, of class XMLUtils.
   */
  @Test
  public void testPrettyPrintWithDOM3LS_String() {
    LOGGER.info("prettyPrintWithDOM3LS");
    String result = XMLUtils.prettyPrintWithDOM3LS(XML_STRING);
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<feed>\n"
            + "    <entry xmlns=\"http://www.w3.org/2005/Atom\">\n"
            + "        <id>tag:search.twitter.com,2005:190215834591956992</id>\n"
            + "        <published>2012-04-11T23:12:43Z</published>\n"
            + "        <link\n"
            + "            href=\"http://twitter.com/David_Wilen/statuses/190215834591956992\"\n"
            + "            rel=\"alternate\" type=\"text/html\"/>\n"
            + "        <title>I'm at Elvis Restaurant (Baltimore, MD) http://t.co/ioDMMZ34</title>\n"
            + "        <content type=\"html\">I'm at &lt;em&gt;Elvis&lt;/em&gt; Restaurant (Baltimore, MD) &lt;a href=\"http://t.co/ioDMMZ34\"&gt;http://t.co/ioDMMZ34&lt;/a&gt;</content>\n"
            + "        <updated>2012-04-11T23:12:43Z</updated>\n"
            + "        <link\n"
            + "            href=\"http://a0.twimg.com/profile_images/1781430382/image_normal.jpg\"\n"
            + "            rel=\"image\" type=\"image/png\"/>\n"
            + "        <twitter:geo xmlns:twitter=\"http://api.twitter.com/\"/>\n"
            + "        <twitter:metadata xmlns:twitter=\"http://api.twitter.com/\">\n"
            + "            <twitter:result_type>recent</twitter:result_type>\n"
            + "        </twitter:metadata>\n"
            + "        <twitter:source xmlns:twitter=\"http://api.twitter.com/\">&lt;a href=\"http://foursquare.com\" rel=\"nofollow\"&gt;foursquare&lt;/a&gt;</twitter:source>\n"
            + "        <twitter:lang xmlns:twitter=\"http://api.twitter.com/\">en</twitter:lang>\n"
            + "        <author>\n"
            + "            <name>David_Wilen (David Wilen)</name>\n"
            + "            <uri>http://twitter.com/David_Wilen</uri>\n"
            + "        </author>\n"
            + "    </entry>\n"
            + "</feed>\n", result);
  }
}
