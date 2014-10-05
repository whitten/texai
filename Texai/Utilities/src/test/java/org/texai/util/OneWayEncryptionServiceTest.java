/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.util;

import org.apache.log4j.Logger;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author reed
 */
public class OneWayEncryptionServiceTest {
  // the logger
  private static final Logger LOGGER = Logger.getLogger(OneWayEncryptionServiceTest.class);

  public OneWayEncryptionServiceTest() {
  }

  /**
   * Test of encrypt method, of class OneWayEncryptionService.
   */
  @Test
  public void testEncrypt1() {
    LOGGER.info("encrypt1");
    String plaintext = "mypassword";
    OneWayEncryptionService instance = OneWayEncryptionService.getInstance();
    String result = instance.encrypt(plaintext);
    assertEquals("ozb2cQgPv08qIw8xNWDd8NDBLfzxdB5J6HIqI0ZzA33Ek8qo0pHYAl9xCJ1jzqgJzIrlPlsXBUgGg32+QJnEyg==", result);
    assertEquals(result, instance.encrypt(plaintext));
  }

  /**
   * Test of encrypt method, of class OneWayEncryptionService.
   */
  @Test
  public void testEncrypt2() {
    LOGGER.info("encrypt2");
    String plaintext = "mypassword";
    String salt = "random salt value";
    OneWayEncryptionService instance = OneWayEncryptionService.getInstance();
    String result = instance.encrypt(plaintext, salt);
    assertEquals("Ua6himZBPVTQxJFhkSAEe0+lEMpfXtvLutQgJT3QfLaxIu02ytYXJqXJubDXAyZwU8mm8ayGEqeAMrXqV1OWPg==", result);
    assertEquals(result, instance.encrypt(plaintext, salt));
  }

  /**
   * Test of encrypt method, of class OneWayEncryptionService.
   */
  @Test
  public void testEncrypt3() {
    LOGGER.info("encrypt3");
    long nbr = 12345;
    OneWayEncryptionService instance = OneWayEncryptionService.getInstance();
    String result = instance.encrypt(nbr);
    assertEquals("1rrl+iL6HzRUpyIhfBr+TKDhSfAZzxaic9Q67ggfNNbtHcN5TtE4MagrPIZ+GtZy6iwT7Hx8tgDLIE2ac6bXIw==", result);
    assertEquals(result, instance.encrypt(nbr));
  }

  /**
   * Test of encrypt method, of class OneWayEncryptionService.
   */
  @Test
  public void testEncrypt4() {
    LOGGER.info("encrypt4");
    long nbr = 12345;
    String salt = "random salt value";
    OneWayEncryptionService instance = OneWayEncryptionService.getInstance();
    String result = instance.encrypt(nbr, salt);
    assertEquals("YQzX8kH9DJ/8LfOvzDWlu/5s/t2hqF9M6ity/OhmJYXgE8HZ0Q2AGvlwIkQYCbV4RrCHi31wCQbXgd2AjqZ3+w==", result);
    assertEquals(result, instance.encrypt(nbr, salt));
  }

}
