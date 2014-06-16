/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.texai.util;

import junit.framework.TestCase;

/**
 *
 * @author reed
 */
public class OneWayEncryptionServiceTest extends TestCase {
    
    public OneWayEncryptionServiceTest(String testName) {
        super(testName);
    }            

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

  /**
   * Test of encrypt method, of class OneWayEncryptionService.
   */
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
  public void testEncrypt2() {
    System.out.println("encrypt");
    long nbr = 12345;
    OneWayEncryptionService instance = OneWayEncryptionService.getInstance();
    String result = instance.encrypt(nbr);
    assertEquals("J4adzFBMRmR2U7s1rgwqOQwIDYE=", result);
    assertEquals(result, instance.encrypt(nbr));
  }

}
