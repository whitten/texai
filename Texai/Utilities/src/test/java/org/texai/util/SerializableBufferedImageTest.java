/*
 * SerializableBufferedImageTest.java
 *
 * Created on Jun 30, 2008, 12:52:24 PM
 *
 * Description: Provides a serializable buffered image.
 *
 * Copyright (C) Dec 12, 2011 reed.
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

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import org.apache.log4j.Logger;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Provides a serializable buffered image.
 *
 * @author reed
 */
public class SerializableBufferedImageTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(SerializableBufferedImageTest.class);

  public SerializableBufferedImageTest() {
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
   * Test of class SerializableBufferedImage.
   */
  @Test
  public void test1() {
    LOGGER.info("color SerializableBufferedImage");
    try {
      BufferedImage bufferedImage = ImageIO.read(new File("data/webcam_stephenreed.jpg"));
      SerializableBufferedImage serializableBufferedImage = new SerializableBufferedImage(bufferedImage);
      assertEquals("[3 byte RGB 320x240]", serializableBufferedImage.toString());
      BufferedImage bufferedImage2 = serializableBufferedImage.getImage();
      assertEquals(bufferedImage.getType(), bufferedImage2.getType());
      assertEquals(bufferedImage.getWidth(), bufferedImage2.getWidth());
      assertEquals(bufferedImage.getHeight(), bufferedImage2.getHeight());

      try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream("data/image.ser"))) {
        objectOutputStream.writeObject(serializableBufferedImage);
      }
      ObjectInputStream objectInputStream = new ObjectInputStream(
              new FileInputStream("data/image.ser"));
      SerializableBufferedImage serializableBufferedImage2 = (SerializableBufferedImage) objectInputStream.readObject();

      assertEquals("[3 byte RGB 320x240]", serializableBufferedImage2.toString());
      BufferedImage bufferedImage3 = serializableBufferedImage.getImage();
      assertEquals(bufferedImage.getType(), bufferedImage3.getType());
      assertEquals(bufferedImage.getWidth(), bufferedImage3.getWidth());
      assertEquals(bufferedImage.getHeight(), bufferedImage3.getHeight());

      File outputfile = new File("data/webcam_stephenreed2.jpg");
      ImageIO.write(bufferedImage, "jpg", outputfile);

    } catch (IOException | ClassNotFoundException ex) {
      LOGGER.error(StringUtils.getStackTraceAsString(ex));
      fail(ex.getMessage());
    }
  }

  /**
   * Test of class SerializableBufferedImage.
   */
  @Test
  public void test2() {
    LOGGER.info("greyscale SerializableBufferedImage");
    try {
      BufferedImage bufferedImage = ImageIO.read(new File("data/stephen-reed_13.jpg"));
      SerializableBufferedImage serializableBufferedImage = new SerializableBufferedImage(bufferedImage);
      assertEquals("[greyscale 92x112]", serializableBufferedImage.toString());
      BufferedImage bufferedImage2 = serializableBufferedImage.getImage();
      assertEquals(bufferedImage.getType(), bufferedImage2.getType());
      assertEquals(bufferedImage.getWidth(), bufferedImage2.getWidth());
      assertEquals(bufferedImage.getHeight(), bufferedImage2.getHeight());

      try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream("data/image.ser"))) {
        objectOutputStream.writeObject(serializableBufferedImage);
      }
      ObjectInputStream objectInputStream = new ObjectInputStream(
              new FileInputStream("data/image.ser"));
      SerializableBufferedImage serializableBufferedImage2 = (SerializableBufferedImage) objectInputStream.readObject();

      assertEquals("[greyscale 92x112]", serializableBufferedImage2.toString());
      BufferedImage bufferedImage3 = serializableBufferedImage.getImage();
      assertEquals(bufferedImage.getType(), bufferedImage3.getType());
      assertEquals(bufferedImage.getWidth(), bufferedImage3.getWidth());
      assertEquals(bufferedImage.getHeight(), bufferedImage3.getHeight());

      File outputfile = new File("data/stephen-reed_13_2.jpg");
      ImageIO.write(bufferedImage, "jpg", outputfile);

    } catch (IOException | ClassNotFoundException ex) {
      LOGGER.error(StringUtils.getStackTraceAsString(ex));
      fail(ex.getMessage());
    }
  }
}
