/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.x509;

import java.io.File;
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
public class MessageDigestUtilsTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(MessageDigestUtilsTest.class);

  public MessageDigestUtilsTest() {
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
   * Test of fileHashString and verifyFileHash methods of class MessageDigestUtils.
   */
  @Test
  public void testFileHashString() {
    LOGGER.info("fileHashString");
    final String fileHashString = "XZSke8hxBfjSYmZqe2QWWPpKzzGohhbpJV04yTjuCA9NWQD1D0NsyytBKBCnH1PcmVuPnUFG6l+DKOpYwzJOgA==";
    assertEquals(fileHashString, MessageDigestUtils.fileHashString("data/SignatureTest.txt"));
    try {
      MessageDigestUtils.verifyFileHash("data/SignatureTest.txt", fileHashString);
    } catch (Exception ex) {
      LOGGER.info(ex.getMessage());
      fail();
    }
    try {
      final String wrongFileHashString = "AZSke8hxBfjSYmZqe2QWWPpKzzGohhbpJV04yTjuCA9NWQD1D0NsyytBKBCnH1PcmVuPnUFG6l+DKOpYwzJOgA==";
      MessageDigestUtils.verifyFileHash("data/SignatureTest.txt", wrongFileHashString);
      fail();
    } catch (Exception ex) {
      LOGGER.info("expected exception occurred");
    }
  }

  /**
   * Test of bytesHashString method, of class MessageDigestUtils.
   */
  @Test
  public void testBytesHashString() {
    LOGGER.info("bytesHashString");
    final byte[] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    assertEquals("D4nuH8t7Ck94CdEmegKXGQBMWl5ewyOnw1I6IJdPmj8gL1b626TNno1lSrny6W3Fx5XqF2+iDt6NhUw0L5A1Mw==", MessageDigestUtils.bytesHashString(bytes));
    final byte[] bytes2 = {1, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    assertEquals("gkruDksK+P07YK/3wurs92TN2+py+EXgdKd0homxlsvfuunDJdtrvfLO6FYpfbCIr7Yve6i/NiQwT78sBK3NBA==", MessageDigestUtils.bytesHashString(bytes2));
  }

}
