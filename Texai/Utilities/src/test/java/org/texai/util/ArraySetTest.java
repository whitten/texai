/*
 * ArraySetTest.java
 * JUnit based test
 *
 * Created on August 25, 2007, 12:02 AM
 */
package org.texai.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import junit.framework.*;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 *
 * @author reed
 */
public class ArraySetTest extends TestCase {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ArraySetTest.class);

  public ArraySetTest(String testName) {
    super(testName);
  }

  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  /**
   * Test of iterator method, of class org.texai.util.ArraySet.
   */
  public void testIterator() {
    LOGGER.info("iterator");

    ArraySet<Integer> instance = new ArraySet<Integer>();
    instance.add(1);
    instance.add(2);
    Iterator<Integer> result = instance.iterator();
    assertTrue(result.hasNext());
    assertTrue(1 == result.next());
    assertTrue(result.hasNext());
    assertTrue(2 == result.next());
    assertTrue(!result.hasNext());
  }

  /**
   * Test of size method, of class org.texai.util.ArraySet.
   */
  public void testSize() {
    LOGGER.info("size");

    ArraySet<Integer> instance = new ArraySet<Integer>();
    instance.add(1);
    instance.add(2);
    int result = instance.size();
    assertEquals(2, result);
  }

  /**
   * Test of add method, of class org.texai.util.ArraySet.
   */
  @SuppressWarnings("unchecked")
  public void testAdd() {
    LOGGER.info("add");

    ArraySet<Integer> instance = new ArraySet<Integer>();
    instance.add(1);
    assertEquals("[1]", instance.toString());
    instance.add(1);
    assertEquals("[1]", instance.toString());
    instance.add(2);
    assertEquals("[1, 2]", instance.toString());
  }

  /**
   * Test of remove method, of class org.texai.util.ArraySet.
   */
  public void testRemove() {
    LOGGER.info("remove");

    ArraySet<Integer> instance = new ArraySet<Integer>();
    instance.add(1);
    instance.add(2);
    assertEquals("[1, 2]", instance.toString());
    instance.remove(1);
    assertEquals("[2]", instance.toString());
  }

  /**
   * Test of isEmpty method, of class org.texai.util.ArraySet.
   */
  public void testIsEmpty() {
    LOGGER.info("isEmpty");

    ArraySet<Integer> instance = new ArraySet<Integer>();
    assertTrue(instance.isEmpty());
    instance.add(1);
    assertTrue(!instance.isEmpty());
    instance.remove(1);
    assertTrue(instance.isEmpty());
  }

  /**
   * Test of contains method, of class org.texai.util.ArraySet.
   */
  public void testContains() {
    LOGGER.info("contains");

    ArraySet<Integer> instance = new ArraySet<Integer>();
    assertTrue(!instance.contains(1));
    instance.add(1);
    assertTrue(instance.contains(1));
    instance.add(2);
    assertTrue(instance.contains(1));
    assertTrue(instance.contains(2));
  }

  /**
   * Test of clear method, of class org.texai.util.ArraySet.
   */
  public void testClear() {
    LOGGER.info("clear");

    ArraySet<Integer> instance = new ArraySet<Integer>();
    instance.add(1);
    instance.add(2);
    assertTrue(!instance.isEmpty());
    instance.clear();
    assertTrue(instance.isEmpty());
  }

  /**
   * Test of clone method, of class org.texai.util.ArraySet.
   */
  @SuppressWarnings({"unchecked", "unchecked"})
  public void testClone() {
    LOGGER.info("clone");

    ArraySet<Integer> instance = new ArraySet<Integer>();
    instance.add(1);
    instance.add(2);
    Object clone = null;
    try {
      clone = (ArraySet<Integer>) instance.clone();
    } catch (Exception ex) {
      fail(ex.getMessage());
    }
    assertTrue(clone instanceof ArraySet<?>);
    assertEquals(instance.toString(), clone.toString());
  }

  /**
   * Test serialization.
   */
  public void testSerialization() {
    LOGGER.info("serialization");
    ObjectOutputStream objectOutputStream = null;
    try {
      ArraySet<Integer> instance = new ArraySet<Integer>();
      instance.add(1);
      instance.add(2);

      // serialize
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      objectOutputStream.writeObject(instance);
      objectOutputStream.close();
      assertTrue(byteArrayOutputStream.toByteArray().length > 0);

      // deserialize
      byte[] pickled = byteArrayOutputStream.toByteArray();
      InputStream in = new ByteArrayInputStream(pickled);
      ObjectInputStream objectInputStream = new ObjectInputStream(in);
      Object result = objectInputStream.readObject();
      assertEquals("[1, 2]", result.toString());
      assertEquals(result, instance);

    } catch (Exception ex) {
      fail(ex.getMessage());
    } finally {
      try {
        objectOutputStream.close();
      } catch (IOException ex) {
        fail(ex.getMessage());
      }
    }

  }
}
