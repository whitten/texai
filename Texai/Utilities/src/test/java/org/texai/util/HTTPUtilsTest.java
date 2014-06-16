/*
 * HTTPUtilsTest.java
 *
 * Created on Jun 30, 2008, 9:23:50 AM
 *
 * Description: .
 *
 * Copyright (C) Jan 29, 2010 reed.
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

import java.util.Arrays;
import java.io.ByteArrayInputStream;
import java.io.PushbackInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
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
public class HTTPUtilsTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(HTTPUtilsTest.class);

  public HTTPUtilsTest() {
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
   * Test of consumeHTTPMessage method, of class HTTPUtils.
   */
  @Test
  public void testConsumeHTTPMessage() {
    LOGGER.info("consumeHTTPMessage");
    final String message1 =
            "HTTP/1.1 200 OK\r\n" +
            "Content-Type: text/html; charset=utf-8\r\n" +
            "Connection: keep-alive\r\n" +
            "Content-Length: 25\r\n" +
            "\r\n" +
            "ignoring unnecessary peer";
    PushbackInputStream pushbackInputStream;
    byte[] result;
    pushbackInputStream = new PushbackInputStream(new ByteArrayInputStream(message1.getBytes()), HTTPUtils.HTTP_RESPONSE_BUFFER_SIZE);
    result = HTTPUtils.consumeHTTPMessage(pushbackInputStream);
    assertEquals(message1, new String(result));

    final String message2 = message1 + "extra bytes";
    pushbackInputStream = new PushbackInputStream(new ByteArrayInputStream(message2.getBytes()), HTTPUtils.HTTP_RESPONSE_BUFFER_SIZE);
    result = HTTPUtils.consumeHTTPMessage(pushbackInputStream);
    assertEquals(message1, new String(result));
  }

  /**
   * Test of getQueryMap method, of class HTTPUtils.
   */
  @Test
  public void testGetQueryMap() {
    LOGGER.info("getQueryMap");
    URI uri = null;
    try {
      uri = new URI("http://127.0.0.1:8088/torrent-tracker/announce?info_hash=7%09%D5QNd%80%0F%1A1%BB%01y%E68%8D9%3EL%AD&peer_id=-SN1000-zYDrC20WtdBw&port=8088&uploaded=0&downloaded=0&left=0&compact=1&ip=192.168.0.4&event=started");
    } catch (URISyntaxException ex) {
      fail(ex.getMessage());
    }
    final String query = uri.getRawQuery();
    assertEquals("info_hash=7%09%D5QNd%80%0F%1A1%BB%01y%E68%8D9%3EL%AD&peer_id=-SN1000-zYDrC20WtdBw&port=8088&uploaded=0&downloaded=0&left=0&compact=1&ip=192.168.0.4&event=started", query);
    final Map<String, String> parameterDictionary = HTTPUtils.getQueryMap(query);
    assertEquals("{port=8088, peer_id=-SN1000-zYDrC20WtdBw, compact=1, event=started, info_hash=7%09%D5QNd%80%0F%1A1%BB%01y%E68%8D9%3EL%AD, uploaded=0, left=0, downloaded=0, ip=192.168.0.4}", parameterDictionary.toString());
  }

  /**
   * Test of getOS method, of class HTTPUtils.
   */
  @Test
  public void testGetOS() {
    LOGGER.info("getOS");
    assertEquals("[<b>?</b>, <b>?</b>, <b>?</b>]", Arrays.asList(HTTPUtils.getOS("")).toString());
    assertEquals("[Win, Vista, VistaNT 6.0]", Arrays.asList(HTTPUtils.getOS("Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0)")).toString());
    assertEquals("[Win, WinXP, WinNT 5.1]", Arrays.asList(HTTPUtils.getOS("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)")).toString());
    assertEquals("[Win, Seven, Seven NT 5.0]", Arrays.asList(HTTPUtils.getOS("Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0 )")).toString());
    assertEquals("[Win, Win98, Win98]", Arrays.asList(HTTPUtils.getOS("Mozilla/4.0 (compatible; MSIE 5.5; Windows 98; Win 9x 4.90)")).toString());
    assertEquals("[Win, WinXP, WinNT 5.1]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.1) Gecko/2008070208 Firefox/3.0.1")).toString());
    assertEquals("[Win, WinXP, WinNT 5.1]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14")).toString());
    assertEquals("[Win, WinXP, WinNT 5.1]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Chrome/0.2.149.29 Safari/525.13")).toString());
    assertEquals("[Win, Vista, VistaNT 6.0]", Arrays.asList(HTTPUtils.getOS("Mozilla/4.8 [en] (Windows NT 6.0; U)")).toString());
    assertEquals("[Win, WinXP, WinNT 5.1]", Arrays.asList(HTTPUtils.getOS("Mozilla/4.8 [en] (Windows NT 5.1; U)")).toString());
    assertEquals("[Win, Vista, VistaNT 6.0]", Arrays.asList(HTTPUtils.getOS("Opera/9.25 (Windows NT 6.0; U; en)")).toString());
    assertEquals("[Win, Seven, Seven NT 5.0]", Arrays.asList(HTTPUtils.getOS("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0; en) Opera 8.0")).toString());
    assertEquals("[Win, WinXP, WinNT 5.1]", Arrays.asList(HTTPUtils.getOS("Opera/7.51 (Windows NT 5.1; U) [en]")).toString());
    assertEquals("[Win, WinXP, WinXP]", Arrays.asList(HTTPUtils.getOS("Opera/7.50 (Windows XP; U)")).toString());
    assertEquals("[<b>?</b>, <b>?</b>, <b>?</b>]", Arrays.asList(HTTPUtils.getOS("Avant Browser/1.2.789rel1 (http://www.avantbrowser.com)")).toString());
    assertEquals("[Win, Win98, Win98]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (Windows; U; Win98; en-US; rv:1.4) Gecko Netscape/7.1 (ax)")).toString());
    assertEquals("[Win, WinXP, WinXP]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (Windows; U; Windows XP) Gecko MultiZilla/1.6.1.0a")).toString());
    assertEquals("[Win, WinME, WinME]", Arrays.asList(HTTPUtils.getOS("Opera/7.50 (Windows ME; U) [en]")).toString());
    assertEquals("[Win, Win95, Win95]", Arrays.asList(HTTPUtils.getOS("Mozilla/3.01Gold (Win95; I)")).toString());
    assertEquals("[Win, Win95, Win95]", Arrays.asList(HTTPUtils.getOS("Mozilla/2.02E (Win95; U)")).toString());
    assertEquals("[Mac, MacOSX, MacOS ]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en) AppleWebKit/125.2 (KHTML, like Gecko) Safari/125.8")).toString());
    assertEquals("[Mac, MacOSX, MacOS ]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en) AppleWebKit/125.2 (KHTML, like Gecko) Safari/85.8")).toString());
    assertEquals("[Mac, MacPPC, MacOS _PowerPC]", Arrays.asList(HTTPUtils.getOS("Mozilla/4.0 (compatible; MSIE 5.15; Mac_PowerPC)")).toString());
    assertEquals("[Mac, MacOSX, MacOS Mach-O]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (Macintosh; U; PPC Mac OS X Mach-O; en-US; rv:1.7a) Gecko/20050614 Firefox/0.9.0+")).toString());
    assertEquals("[Mac, MacOSX, MacOS ]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en-US) AppleWebKit/125.4 (KHTML, like Gecko, Safari) OmniWeb/v563.15")).toString());
    assertEquals("[Linux, Linux, Linux ]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (X11; U; Linux; i686; en-US; rv:1.6) Gecko Debian/1.6-7")).toString());
    assertEquals("[Linux, Linux, Linux ]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (X11; U; Linux; i686; en-US; rv:1.6) Gecko Epiphany/1.2.5")).toString());
    assertEquals("[Linux, Linux, Linux i586]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (X11; U; Linux i586; en-US; rv:1.7.3) Gecko/20050924 Epiphany/1.4.4 (Ubuntu)")).toString());
    assertEquals("[Linux, Linux, Linux ]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (compatible; Konqueror/3.5; Linux) KHTML/3.5.10 (like Gecko) (Kubuntu)")).toString());
    assertEquals("[Linux, Linux, Linux ]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (X11; U; Linux; i686; en-US; rv:1.6) Gecko Galeon/1.3.14")).toString());
    assertEquals("[Linux, Linux, Linux ]", Arrays.asList(HTTPUtils.getOS("Konqueror/3.0-rc4; (Konqueror/3.0-rc4; i686 Linux;;datecode)")).toString());
    assertEquals("[Linux, Linux, Linux 2.6.8-gentoo-r3]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (compatible; Konqueror/3.3; Linux 2.6.8-gentoo-r3; X11;")).toString());
    assertEquals("[Linux, Linux, Linux i686]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.6) Gecko/20050614 Firefox/0.8")).toString());
    assertEquals("[Linux, Linux, Linux 2.6.9-kanotix-8 i686]", Arrays.asList(HTTPUtils.getOS("ELinks/0.9.3 (textmode; Linux 2.6.9-kanotix-8 i686; 127x41)")).toString());
    assertEquals("[Linux, Linux, Linux 2.6.10-ac7 i686]", Arrays.asList(HTTPUtils.getOS("ELinks (0.4pre5; Linux 2.6.10-ac7 i686; 80x33)")).toString());
    assertEquals("[Linux, Linux, Linux 2.4.26 i686]", Arrays.asList(HTTPUtils.getOS("Links (2.1pre15; Linux 2.4.26 i686; 158x61)")).toString());
    assertEquals("[Linux, Linux, Linux 2.4.24]", Arrays.asList(HTTPUtils.getOS("Links/0.9.1 (Linux 2.4.24; i386;)")).toString());
    assertEquals("[Linux, Linux, Linux ]", Arrays.asList(HTTPUtils.getOS("MSIE (MSIE 6.0; X11; Linux; i686) Opera 7.23")).toString());
    assertEquals("[Linux, Linux, Linux i686]", Arrays.asList(HTTPUtils.getOS("Opera/9.52 (X11; Linux i686; U; en)")).toString());
    assertEquals("[<b>?</b>, <b>?</b>, <b>?</b>]", Arrays.asList(HTTPUtils.getOS("Lynx/2.8.5rel.1 libwww-FM/2.14 SSL-MM/1.4.1 GNUTLS/0.8.12")).toString());
    assertEquals("[<b>?</b>, <b>?</b>, <b>?</b>]", Arrays.asList(HTTPUtils.getOS("w3m/0.5.1")).toString());
    assertEquals("[*BSD, *BSD FreeBSD, FreeBSD 5.3-RELEASE i386]", Arrays.asList(HTTPUtils.getOS("Links (2.1pre15; FreeBSD 5.3-RELEASE i386; 196x84)")).toString());
    assertEquals("[*BSD, *BSD FreeBSD, FreeBSD ]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (X11; U; FreeBSD; i386; en-US; rv:1.7) Gecko")).toString());
    assertEquals("[Unix, IRIX, IRIX]", Arrays.asList(HTTPUtils.getOS("Mozilla/4.77 [en] (X11; I; IRIX;64 6.5 IP30)")).toString());
    assertEquals("[Unix, SunOS, SunOS]", Arrays.asList(HTTPUtils.getOS("Mozilla/4.8 [en] (X11; U; SunOS; 5.7 sun4u)")).toString());
    assertEquals("[BeOS, BeOS, BeOS]", Arrays.asList(HTTPUtils.getOS("Mozilla/3.0 (compatible; NetPositive/2.1.1; BeOS)")).toString());
    assertEquals("[Bot, Bot, Bot]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")).toString());
    assertEquals("[Bot, Bot, Bot]", Arrays.asList(HTTPUtils.getOS("Googlebot/2.1 (+http://www.googlebot.com/bot.html)")).toString());
    assertEquals("[Bot, Bot, Bot]", Arrays.asList(HTTPUtils.getOS("msnbot/1.0 (+http://search.msn.com/msnbot.htm)")).toString());
    assertEquals("[Bot, Bot, Bot]", Arrays.asList(HTTPUtils.getOS("msnbot/0.11 (+http://search.msn.com/msnbot.htm)")).toString());
    assertEquals("[Bot, Bot, Bot]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (compatible; Yahoo! Slurp; http://help.yahoo.com/help/us/ysearch/slurp)")).toString());
    assertEquals("[Bot, Bot, Bot]", Arrays.asList(HTTPUtils.getOS("Mozilla/2.0 (compatible; Ask Jeeves/Teoma)")).toString());
    assertEquals("[Bot, Bot, Bot]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (compatible; ScoutJet; +http://www.scoutjet.com/)")).toString());
    assertEquals("[Bot, Bot, Bot]", Arrays.asList(HTTPUtils.getOS("Gulper Web Bot 0.2.4 (www.ecsl.cs.sunysb.edu/~maxim/cgi-bin/Link/GulperBot)")).toString());
    assertEquals("[<b>?</b>, <b>?</b>, <b>?</b>]", Arrays.asList(HTTPUtils.getOS("EmailWolf 1.00")).toString());
    assertEquals("[<b>?</b>, <b>?</b>, <b>?</b>]", Arrays.asList(HTTPUtils.getOS("grub-client-1.5.3; (grub-client-1.5.3; Crawl your own stuff with http://grub.org)")).toString());
    assertEquals("[<b>?</b>, <b>?</b>, <b>?</b>]", Arrays.asList(HTTPUtils.getOS("Download Demon/3.5.0.11")).toString());
    assertEquals("[<b>?</b>, <b>?</b>, <b>?</b>]", Arrays.asList(HTTPUtils.getOS("Microsoft URL Control - 6.00.8862")).toString());
    assertEquals("[<b>?</b>, <b>?</b>, <b>?</b>]", Arrays.asList(HTTPUtils.getOS("OmniWeb/2.7-beta-3 OWF/1.0")).toString());
    assertEquals("[<b>?</b>, <b>?</b>, <b>?</b>]", Arrays.asList(HTTPUtils.getOS("Mozilla/3.0 (compatible; HandHTTP 1.1)")).toString());
    assertEquals("[Win, <b>WinNT?</b>, <b>WinNT?</b>]", Arrays.asList(HTTPUtils.getOS("Mozilla/4.0 (compatible; MSIE 4.01; Windows NT Windows CE)")).toString());
    assertEquals("[Win, WinCE, WinCE]", Arrays.asList(HTTPUtils.getOS("Mozilla/2.0 (compatible; MSIE 3.02; Windows CE; 240x320)")).toString());
    assertEquals("[<b>?</b>, <b>?</b>, <b>?</b>]", Arrays.asList(HTTPUtils.getOS("Mozilla/1.22 (compatible; MMEF20; Cellphone; Sony CMD-Z5)")).toString());
    assertEquals("[Mac, MacOSX-iPhone, MacOS-iPhone ]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (iPhone; U; XXXXX like Mac OS X; en) AppleWebKit/420+ (KHTML, like Gecko) Version/3.0 Mobile/241 Safari/419.3")).toString());
    assertEquals("[Android, Android, Android 1.6]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (Linux; U; Android 1.6; en-gb; Dell Streak Build/Donut AppleWebKit/528.5+ (KHTML, like Gecko) Version/3.1.2 Mobile Safari/ 525.20.1")).toString());
    assertEquals("[Android, Android, Android 2.2]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (Linux; U; Android 2.2; nl-nl; Desire_A8181 Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1")).toString());
    assertEquals("[BlackBerry, BlackBerry, BlackBerry]", Arrays.asList(HTTPUtils.getOS("Mozilla/5.0 (BlackBerry; U; BlackBerry 9800; en) AppleWebKit/534.1+ (KHTML, Like Gecko) Version/6.0.0.141 Mobile Safari/534.1+")).toString());
    assertEquals("[BlackBerry, BlackBerry, BlackBerry9630]", Arrays.asList(HTTPUtils.getOS("BlackBerry9630/4.7.1.40 Profile/MIDP-2.0 Configuration/CLDC-1.1 VendorID/105")).toString());
    assertEquals("[Linux, Linux, Linux 2.6.22]", Arrays.asList(HTTPUtils.getOS("Mozilla/4.0 (compatible; Linux 2.6.22) NetFront/3.4 Kindle/2.0 (screen 600x800)")).toString());
  }

  /**
   * Test of getBotName method, of class HTTPUtils.
   */
  @Test
  public void testGetBotName() {
    LOGGER.info("getBotName");
    assertNull(HTTPUtils.getBotName(""));
    assertNull(HTTPUtils.getBotName("Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0)"));
    assertNull(HTTPUtils.getBotName("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)"));
    assertNull(HTTPUtils.getBotName("Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0 )"));
    assertNull(HTTPUtils.getBotName("Mozilla/4.0 (compatible; MSIE 5.5; Windows 98; Win 9x 4.90)"));
    assertNull(HTTPUtils.getBotName("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.1) Gecko/2008070208 Firefox/3.0.1"));
    assertNull(HTTPUtils.getBotName("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14"));
    assertNull(HTTPUtils.getBotName("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Chrome/0.2.149.29 Safari/525.13"));
    assertNull(HTTPUtils.getBotName("Mozilla/4.8 [en] (Windows NT 6.0; U)"));
    assertNull(HTTPUtils.getBotName("Mozilla/4.8 [en] (Windows NT 5.1; U)"));
    assertNull(HTTPUtils.getBotName("Opera/9.25 (Windows NT 6.0; U; en)"));
    assertNull(HTTPUtils.getBotName("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0; en) Opera 8.0"));
    assertNull(HTTPUtils.getBotName("Opera/7.51 (Windows NT 5.1; U) [en]"));
    assertNull(HTTPUtils.getBotName("Opera/7.50 (Windows XP; U)"));
    assertNull(HTTPUtils.getBotName("Avant Browser/1.2.789rel1 (http://www.avantbrowser.com)"));
    assertNull(HTTPUtils.getBotName("Mozilla/5.0 (Windows; U; Win98; en-US; rv:1.4) Gecko Netscape/7.1 (ax)"));
    assertNull(HTTPUtils.getBotName("Mozilla/5.0 (Windows; U; Windows XP) Gecko MultiZilla/1.6.1.0a"));
    assertNull(HTTPUtils.getBotName("Opera/7.50 (Windows ME; U) [en]"));
    assertNull(HTTPUtils.getBotName("Mozilla/3.01Gold (Win95; I)"));
    assertNull(HTTPUtils.getBotName("Mozilla/2.02E (Win95; U)"));
    assertNull(HTTPUtils.getBotName("Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en) AppleWebKit/125.2 (KHTML, like Gecko) Safari/125.8"));
    assertNull(HTTPUtils.getBotName("Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en) AppleWebKit/125.2 (KHTML, like Gecko) Safari/85.8"));
    assertNull(HTTPUtils.getBotName("Mozilla/4.0 (compatible; MSIE 5.15; Mac_PowerPC)"));
    assertNull(HTTPUtils.getBotName("Mozilla/5.0 (Macintosh; U; PPC Mac OS X Mach-O; en-US; rv:1.7a) Gecko/20050614 Firefox/0.9.0+"));
    assertNull(HTTPUtils.getBotName("Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en-US) AppleWebKit/125.4 (KHTML, like Gecko, Safari) OmniWeb/v563.15"));
    assertNull(HTTPUtils.getBotName("Mozilla/5.0 (X11; U; Linux; i686; en-US; rv:1.6) Gecko Debian/1.6-7"));
    assertNull(HTTPUtils.getBotName("Mozilla/5.0 (X11; U; Linux; i686; en-US; rv:1.6) Gecko Epiphany/1.2.5"));
    assertNull(HTTPUtils.getBotName("Mozilla/5.0 (X11; U; Linux i586; en-US; rv:1.7.3) Gecko/20050924 Epiphany/1.4.4 (Ubuntu)"));
    assertNull(HTTPUtils.getBotName("Mozilla/5.0 (compatible; Konqueror/3.5; Linux) KHTML/3.5.10 (like Gecko) (Kubuntu)"));
    assertNull(HTTPUtils.getBotName("Mozilla/5.0 (X11; U; Linux; i686; en-US; rv:1.6) Gecko Galeon/1.3.14"));
    assertNull(HTTPUtils.getBotName("Konqueror/3.0-rc4; (Konqueror/3.0-rc4; i686 Linux;;datecode)"));
    assertNull(HTTPUtils.getBotName("Mozilla/5.0 (compatible; Konqueror/3.3; Linux 2.6.8-gentoo-r3; X11;"));
    assertNull(HTTPUtils.getBotName("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.6) Gecko/20050614 Firefox/0.8"));
    assertNull(HTTPUtils.getBotName("ELinks/0.9.3 (textmode; Linux 2.6.9-kanotix-8 i686; 127x41)"));
    assertNull(HTTPUtils.getBotName("ELinks (0.4pre5; Linux 2.6.10-ac7 i686; 80x33)"));
    assertNull(HTTPUtils.getBotName("Links (2.1pre15; Linux 2.4.26 i686; 158x61)"));
    assertNull(HTTPUtils.getBotName("Links/0.9.1 (Linux 2.4.24; i386;)"));
    assertNull(HTTPUtils.getBotName("MSIE (MSIE 6.0; X11; Linux; i686) Opera 7.23"));
    assertNull(HTTPUtils.getBotName("Opera/9.52 (X11; Linux i686; U; en)"));
    assertNull(HTTPUtils.getBotName("Lynx/2.8.5rel.1 libwww-FM/2.14 SSL-MM/1.4.1 GNUTLS/0.8.12"));
    assertNull(HTTPUtils.getBotName("w3m/0.5.1"));
    assertNull(HTTPUtils.getBotName("Links (2.1pre15; FreeBSD 5.3-RELEASE i386; 196x84)"));
    assertNull(HTTPUtils.getBotName("Mozilla/5.0 (X11; U; FreeBSD; i386; en-US; rv:1.7) Gecko"));
    assertNull(HTTPUtils.getBotName("Mozilla/4.77 [en] (X11; I; IRIX;64 6.5 IP30)"));
    assertNull(HTTPUtils.getBotName("Mozilla/4.8 [en] (X11; U; SunOS; 5.7 sun4u)"));
    assertNull(HTTPUtils.getBotName("Mozilla/3.0 (compatible; NetPositive/2.1.1; BeOS)"));
    assertEquals("[Google, Google, Google2.1]", Arrays.asList(HTTPUtils.getBotName("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")).toString());
    assertEquals("[Google, Google, Google2.1]", Arrays.asList(HTTPUtils.getBotName("Googlebot/2.1 (+http://www.googlebot.com/bot.html)")).toString());
    assertEquals("[MSNBot, MSNBot, MSNBot1.0]", Arrays.asList(HTTPUtils.getBotName("msnbot/1.0 (+http://search.msn.com/msnbot.htm)")).toString());
    assertEquals("[MSNBot, MSNBot, MSNBot0.11]", Arrays.asList(HTTPUtils.getBotName("msnbot/0.11 (+http://search.msn.com/msnbot.htm)")).toString());
    assertEquals("[Yahoo, Yahoo, Yahoo]", Arrays.asList(HTTPUtils.getBotName("Mozilla/5.0 (compatible; Yahoo! Slurp; http://help.yahoo.com/help/us/ysearch/slurp)")).toString());
    assertEquals("[Teoma, Teoma, Teoma]", Arrays.asList(HTTPUtils.getBotName("Mozilla/2.0 (compatible; Ask Jeeves/Teoma)")).toString());
    assertEquals("[ScoutJet, ScoutJet, ScoutJet]", Arrays.asList(HTTPUtils.getBotName("Mozilla/5.0 (compatible; ScoutJet; +http://www.scoutjet.com/)")).toString());
    assertEquals("[Gulper, Gulper, Gulper0.2.4]", Arrays.asList(HTTPUtils.getBotName("Gulper Web Bot 0.2.4 (www.ecsl.cs.sunysb.edu/~maxim/cgi-bin/Link/GulperBot)")).toString());
    assertNull(HTTPUtils.getBotName("EmailWolf 1.00"));
    assertNull(HTTPUtils.getBotName("grub-client-1.5.3; (grub-client-1.5.3; Crawl your own stuff with http://grub.org)"));
    assertNull(HTTPUtils.getBotName("Download Demon/3.5.0.11"));
    assertNull(HTTPUtils.getBotName("Microsoft URL Control - 6.00.8862"));
    assertNull(HTTPUtils.getBotName("OmniWeb/2.7-beta-3 OWF/1.0"));
  }

  /**
   * Test of getBrowser method, of class HTTPUtils.
   */
  @Test
  public void testGetBrowser() {
    LOGGER.info("getBrowser");
    assertEquals("[<b>?</b>, <b>?</b>, <b>?</b>]", Arrays.asList(HTTPUtils.getBrowser("")).toString());
    assertEquals("[MSIE, MSIE7, MSIE7.0]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0)")).toString());
    assertEquals("[MSIE, MSIE6, MSIE6.0]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)")).toString());
    assertEquals("[MSIE, MSIE5.5, MSIE5.5]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0 )")).toString());
    assertEquals("[MSIE, MSIE5.5, MSIE5.5]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/4.0 (compatible; MSIE 5.5; Windows 98; Win 9x 4.90)")).toString());
    assertEquals("[Gecko, Gecko(Firefox), Gecko2008(Firefox3.0.1)]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.1) Gecko/2008070208 Firefox/3.0.1")).toString());
    assertEquals("[Gecko, Gecko(Firefox), Gecko2008(Firefox2.0.0.14)]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14")).toString());
    assertEquals("[KHTML, KHTML(Chrome), KHTML(Chrome0.2.149.29)]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Chrome/0.2.149.29 Safari/525.13")).toString());
    assertEquals("[Communicator, Communicator, Communicator4.8]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/4.8 [en] (Windows NT 6.0; U)")).toString());
    assertEquals("[Communicator, Communicator, Communicator4.8]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/4.8 [en] (Windows NT 5.1; U)")).toString());
    assertEquals("[Opera, Opera9, Opera9.25]", Arrays.asList(HTTPUtils.getBrowser("Opera/9.25 (Windows NT 6.0; U; en)")).toString());
    assertEquals("[Opera, Opera8, Opera8.0]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0; en) Opera 8.0")).toString());
    assertEquals("[Opera, Opera7, Opera7.51]", Arrays.asList(HTTPUtils.getBrowser("Opera/7.51 (Windows NT 5.1; U) [en]")).toString());
    assertEquals("[Opera, Opera7, Opera7.50]", Arrays.asList(HTTPUtils.getBrowser("Opera/7.50 (Windows XP; U)")).toString());
    assertEquals("[Avant, Avant, Avant1.2.789rel1]", Arrays.asList(HTTPUtils.getBrowser("Avant Browser/1.2.789rel1 (http://www.avantbrowser.com)")).toString());
    assertEquals("[NS, NS?, NS?5.0]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (Windows; U; Win98; en-US; rv:1.4) Gecko Netscape/7.1 (ax)")).toString());
    assertEquals("[MultiZilla, MultiZilla, MultiZilla1.6.1.0a)]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (Windows; U; Windows XP) Gecko MultiZilla/1.6.1.0a")).toString());
    assertEquals("[Opera, Opera7, Opera7.50]", Arrays.asList(HTTPUtils.getBrowser("Opera/7.50 (Windows ME; U) [en]")).toString());
    assertEquals("[Mozilla, Mozilla, Mozilla3.01Gold]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/3.01Gold (Win95; I)")).toString());
    assertEquals("[Mozilla, Mozilla, Mozilla2.02E]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/2.02E (Win95; U)")).toString());
    assertEquals("[KHTML, KHTML(Safari), KHTML(Safari125.8)]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en) AppleWebKit/125.2 (KHTML, like Gecko) Safari/125.8")).toString());
    assertEquals("[KHTML, KHTML(Safari), KHTML(Safari85.8)]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en) AppleWebKit/125.2 (KHTML, like Gecko) Safari/85.8")).toString());
    assertEquals("[MSIE, MSIE5.x, MSIE5.15]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/4.0 (compatible; MSIE 5.15; Mac_PowerPC)")).toString());
    assertEquals("[Gecko, Gecko(Firefox), Gecko2005(Firefox0.9.0+)]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (Macintosh; U; PPC Mac OS X Mach-O; en-US; rv:1.7a) Gecko/20050614 Firefox/0.9.0+")).toString());
    assertEquals("[OmniWeb, OmniWeb, OmniWebv563.15]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en-US) AppleWebKit/125.4 (KHTML, like Gecko, Safari) OmniWeb/v563.15")).toString());
    assertEquals("[Debian, Debian, Debian1.6-7]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (X11; U; Linux; i686; en-US; rv:1.6) Gecko Debian/1.6-7")).toString());
    assertEquals("[Epiphany, Epiphany, Epiphany1.2.5]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (X11; U; Linux; i686; en-US; rv:1.6) Gecko Epiphany/1.2.5")).toString());
    assertEquals("[Epiphany, Epiphany, Epiphany1.4.4]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (X11; U; Linux i586; en-US; rv:1.7.3) Gecko/20050924 Epiphany/1.4.4 (Ubuntu)")).toString());
    assertEquals("[KHTML, KHTML(Konqueror), KHTML(Konqueror3.5)]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (compatible; Konqueror/3.5; Linux) KHTML/3.5.10 (like Gecko) (Kubuntu)")).toString());
    assertEquals("[Galeon, Galeon, Galeon1.3.14]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (X11; U; Linux; i686; en-US; rv:1.6) Gecko Galeon/1.3.14")).toString());
    assertEquals("[KHTML, KHTML(Konqueror), KHTML(Konqueror3.0-rc4)]", Arrays.asList(HTTPUtils.getBrowser("Konqueror/3.0-rc4; (Konqueror/3.0-rc4; i686 Linux;;datecode)")).toString());
    assertEquals("[KHTML, KHTML(Konqueror), KHTML(Konqueror3.3)]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (compatible; Konqueror/3.3; Linux 2.6.8-gentoo-r3; X11;")).toString());
    assertEquals("[Gecko, Gecko(Firefox), Gecko2005(Firefox0.8)]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.6) Gecko/20050614 Firefox/0.8")).toString());
    assertEquals("[ELinks, ELinks, ELinks0.9.3]", Arrays.asList(HTTPUtils.getBrowser("ELinks/0.9.3 (textmode; Linux 2.6.9-kanotix-8 i686; 127x41)")).toString());
    assertEquals("[ELinks, ELinks, ELinks0.4pre5]", Arrays.asList(HTTPUtils.getBrowser("ELinks (0.4pre5; Linux 2.6.10-ac7 i686; 80x33)")).toString());
    assertEquals("[Links, Links, Links2.1pre15]", Arrays.asList(HTTPUtils.getBrowser("Links (2.1pre15; Linux 2.4.26 i686; 158x61)")).toString());
    assertEquals("[Links, Links, Links0.9.1]", Arrays.asList(HTTPUtils.getBrowser("Links/0.9.1 (Linux 2.4.24; i386;)")).toString());
    assertEquals("[Opera, Opera7, Opera7.23]", Arrays.asList(HTTPUtils.getBrowser("MSIE (MSIE 6.0; X11; Linux; i686) Opera 7.23")).toString());
    assertEquals("[Opera, Opera9, Opera9.52]", Arrays.asList(HTTPUtils.getBrowser("Opera/9.52 (X11; Linux i686; U; en)")).toString());
    assertEquals("[Lynx, Lynx, Lynx2.8.5rel.1 libwww-FM]", Arrays.asList(HTTPUtils.getBrowser("Lynx/2.8.5rel.1 libwww-FM/2.14 SSL-MM/1.4.1 GNUTLS/0.8.12")).toString());
    assertEquals("[w3m, w3m, w3m0.5.1]", Arrays.asList(HTTPUtils.getBrowser("w3m/0.5.1")).toString());
    assertEquals("[Links, Links, Links2.1pre15]", Arrays.asList(HTTPUtils.getBrowser("Links (2.1pre15; FreeBSD 5.3-RELEASE i386; 196x84)")).toString());
    assertEquals("[Mozilla, Mozilla, Mozilla5.0]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (X11; U; FreeBSD; i386; en-US; rv:1.7) Gecko")).toString());
    assertEquals("[Communicator, Communicator, Communicator4.77]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/4.77 [en] (X11; I; IRIX;64 6.5 IP30)")).toString());
    assertEquals("[Communicator, Communicator, Communicator4.8]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/4.8 [en] (X11; U; SunOS; 5.7 sun4u)")).toString());
    assertEquals("[Mozilla, Mozilla, Mozilla3.0]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/3.0 (compatible; NetPositive/2.1.1; BeOS)")).toString());
    assertEquals("[Google, Google, Google2.1]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")).toString());
    assertEquals("[Google, Google, Google2.1]", Arrays.asList(HTTPUtils.getBrowser("Googlebot/2.1 (+http://www.googlebot.com/bot.html)")).toString());
    assertEquals("[MSNBot, MSNBot, MSNBot1.0]", Arrays.asList(HTTPUtils.getBrowser("msnbot/1.0 (+http://search.msn.com/msnbot.htm)")).toString());
    assertEquals("[MSNBot, MSNBot, MSNBot0.11]", Arrays.asList(HTTPUtils.getBrowser("msnbot/0.11 (+http://search.msn.com/msnbot.htm)")).toString());
    assertEquals("[Yahoo, Yahoo, Yahoo]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (compatible; Yahoo! Slurp; http://help.yahoo.com/help/us/ysearch/slurp)")).toString());
    assertEquals("[Teoma, Teoma, Teoma]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/2.0 (compatible; Ask Jeeves/Teoma)")).toString());
    assertEquals("[ScoutJet, ScoutJet, ScoutJet]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (compatible; ScoutJet; +http://www.scoutjet.com/)")).toString());
    assertEquals("[Gulper, Gulper, Gulper0.2.4]", Arrays.asList(HTTPUtils.getBrowser("Gulper Web Bot 0.2.4 (www.ecsl.cs.sunysb.edu/~maxim/cgi-bin/Link/GulperBot)")).toString());
    assertEquals("[<b>?</b>, <b>?</b>, <b>?</b>]", Arrays.asList(HTTPUtils.getBrowser("EmailWolf 1.00")).toString());
    assertEquals("[<b>?</b>, <b>?</b>, <b>?</b>]", Arrays.asList(HTTPUtils.getBrowser("grub-client-1.5.3; (grub-client-1.5.3; Crawl your own stuff with http://grub.org)")).toString());
    assertEquals("[<b>?</b>, <b>?</b>, <b>?</b>]", Arrays.asList(HTTPUtils.getBrowser("Download Demon/3.5.0.11")).toString());
    assertEquals("[<b>?</b>, <b>?</b>, <b>?</b>]", Arrays.asList(HTTPUtils.getBrowser("Microsoft URL Control - 6.00.8862")).toString());
    assertEquals("[OmniWeb, OmniWeb, OmniWeb2.7-beta-3]", Arrays.asList(HTTPUtils.getBrowser("OmniWeb/2.7-beta-3 OWF/1.0")).toString());
    assertEquals("[HandHTTP, HandHTTP, HandHTTP1.1]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/3.0 (compatible; HandHTTP 1.1)")).toString());
    assertEquals("[MSIE, MSIE4, MSIE4.01]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/4.0 (compatible; MSIE 4.01; Windows NT Windows CE)")).toString());
    assertEquals("[MSIE, <b>MSIE?</b>, <b>MSIE?3.02</b>]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/2.0 (compatible; MSIE 3.02; Windows CE; 240x320)")).toString());
    assertEquals("[Mozilla, Mozilla, Mozilla1.22]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/1.22 (compatible; MMEF20; Cellphone; Sony CMD-Z5)")).toString());
    assertEquals("[KHTML, KHTML(Safari), KHTML(Safari419.3)]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (iPhone; U; XXXXX like Mac OS X; en) AppleWebKit/420+ (KHTML, like Gecko) Version/3.0 Mobile/241 Safari/419.3")).toString());
    assertEquals("[KHTML, KHTML(Safari), KHTML(Safari525.20.1)]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (Linux; U; Android 1.6; en-gb; Dell Streak Build/Donut AppleWebKit/528.5+ (KHTML, like Gecko) Version/3.1.2 Mobile Safari/ 525.20.1")).toString());
    assertEquals("[KHTML, KHTML(Safari), KHTML(Safari533.1)]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (Linux; U; Android 2.2; nl-nl; Desire_A8181 Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1")).toString());
    assertEquals("[KHTML, KHTML(Safari), KHTML(Safari534.1+)]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/5.0 (BlackBerry; U; BlackBerry 9800; en) AppleWebKit/534.1+ (KHTML, Like Gecko) Version/6.0.0.141 Mobile Safari/534.1+")).toString());
    assertEquals("[BlackBerry, BlackBerry, BlackBerry9630]", Arrays.asList(HTTPUtils.getBrowser("BlackBerry9630/4.7.1.40 Profile/MIDP-2.0 Configuration/CLDC-1.1 VendorID/105")).toString());
    assertEquals("[NetFront, NetFront, NetFront 3.4]", Arrays.asList(HTTPUtils.getBrowser("Mozilla/4.0 (compatible; Linux 2.6.22) NetFront/3.4 Kindle/2.0 (screen 600x800)")).toString());
  }

}
