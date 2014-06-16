/*
 * SerializableBufferedImage.java
 *
 * Created on Dec 12, 2011, 12:37:03 PM
 *
 * Description: Provides a serializable buffered image.
 *
 * Copyright (C) Dec 12, 2011, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.util;

import java.awt.image.BufferedImage;
import java.io.Serializable;
import net.jcip.annotations.NotThreadSafe;

/** Provides a serializable buffered image.
 *
 * @author reed
 */
@NotThreadSafe
public class SerializableBufferedImage implements Serializable {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the image type */
  private final int type;
  /** the image byte array */
  private final int[] rgbArray;
  /** the width */
  private final int width;
  /** the height */
  private final int height;

  /** Constructs a new SerializableBufferedImage instance.
   *
   * @param bufferedImage the buffered image
   */
  public SerializableBufferedImage(final BufferedImage bufferedImage) {
    //Preconditions
    assert bufferedImage != null : "bufferedImage must not be null";

    height = bufferedImage.getHeight();
    width = bufferedImage.getWidth();
    rgbArray = new int[width * height];
    type = bufferedImage.getType();

    bufferedImage.getRGB(
            0, // startX
            0, // startY
            width,
            height,
            rgbArray,
            0, // offset
            width); // scansize
  }

  /** returns a buffered image.
   *
   * @return a buffered image
   */
  public BufferedImage getImage() {
    final BufferedImage bufferedImage = new BufferedImage(
            width,
            height,
            type);
    bufferedImage.setRGB(
            0,
            0,
            width,
            height,
            rgbArray,
            0,
            width);
    return bufferedImage;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  public String toString() {
    final String typeString;
    switch (type) {
      case BufferedImage.TYPE_BYTE_GRAY:
        typeString = "greyscale";
        break;
      case BufferedImage.TYPE_3BYTE_BGR:
        typeString = "3 byte RGB";
        break;
      case BufferedImage.TYPE_INT_RGB:
        typeString = "int RGB";
        break;
      default:
        typeString = "image";
    }
    return new StringBuilder().append("[").append(typeString).append(' ').append(width).append('x').append(height).append(']').toString();
  }
}
