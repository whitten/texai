/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author reed
 */
public class OneWayEncryptionServiceTest {

  public OneWayEncryptionServiceTest() {
  }

  /**
   * Test of encrypt method, of class OneWayEncryptionService.
   */
  @Test
  public void testEncrypt1() {
    System.out.println("encrypt");
    String plaintext = "mypassword";
    OneWayEncryptionService instance = OneWayEncryptionService.getInstance();
    String result = instance.encrypt(plaintext);
    assertEquals("kd/Z3bQZiv/FwZTNjObTOP3kcOI=", result);
    assertEquals(result, instance.encrypt(plaintext));
  }

  /**
   * Test of encrypt method, of class OneWayEncryptionService.
   */
  @Test
  public void testEncrypt2() {
    System.out.println("encrypt");
    long nbr = 12345;
    OneWayEncryptionService instance = OneWayEncryptionService.getInstance();
    String result = instance.encrypt(nbr);
    assertEquals("J4adzFBMRmR2U7s1rgwqOQwIDYE=", result);
    assertEquals(result, instance.encrypt(nbr));
  }

}
