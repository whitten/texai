/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.x509;

import java.io.File;
import javax.crypto.SecretKey;
import org.apache.log4j.Logger;
import org.bouncycastle.util.Arrays;
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
public class SymmetricKeyUtilsTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(SymmetricKeyUtilsTest.class);

  public SymmetricKeyUtilsTest() {
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
   * Test of class SymmetricKeyUtils methods.
   */
  @Test
  public void test() {
    LOGGER.info("generateKey");
    SecretKey secretKey = SymmetricKeyUtils.generateKey();
    assertNotNull(secretKey);

    LOGGER.info("saveKey");
    File file = new File("data/aes-key.txt");
    SymmetricKeyUtils.saveKey(secretKey, file);

    LOGGER.info("loadKey");
    SecretKey loadedSecretKey = SymmetricKeyUtils.loadKey(file);
    assertEquals(secretKey, loadedSecretKey);
    assertTrue(file.exists());
    file.delete();
    assertNull(SymmetricKeyUtils.loadKey(file));

    final String plainText = "The quick brown fox jumps over the fence.";
    LOGGER.info("plainText: " + plainText);
    final String encryptedText = SymmetricKeyUtils.encryptBase64(plainText, secretKey);
    LOGGER.info("encryptedText: " + encryptedText);
    assertTrue(!plainText.equals(encryptedText));
    final String decryptedText = SymmetricKeyUtils.decryptBase64(encryptedText, secretKey);
    LOGGER.info("decryptedText: " + decryptedText);
    assertEquals(plainText, decryptedText);

    final byte[] plainTextBytes = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    LOGGER.info("plainTextBytes length: " + plainTextBytes.length);
    final byte[] encryptedTextBytes = SymmetricKeyUtils.encrypt(plainTextBytes, secretKey);
    LOGGER.info("encryptedTextBytes length: " + encryptedTextBytes.length);
    assertTrue(!Arrays.areEqual(plainTextBytes, encryptedTextBytes));
    final byte[] decryptedTextBytes = SymmetricKeyUtils.decrypt(encryptedTextBytes, secretKey);
    assertTrue(Arrays.areEqual(plainTextBytes, decryptedTextBytes));
  }

}
