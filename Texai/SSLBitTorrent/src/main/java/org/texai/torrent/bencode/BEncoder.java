/*
 * BDecoder - Converts an InputStream to BEValues. Copyright (C) 2003 Mark J.
 * Wielaard
 *
 * This file is part of Snark.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * Revised by Stephen L. Reed, Dec 22, 2009.
 * Reformatted, fixed Checkstyle, Findbugs and PMD violations, and substituted Log4J logger
 * for consistency with the Texai project.
 */
package org.texai.torrent.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Converts an InputStream to BEValues. */
public final class BEncoder {

  /** Constructs a new BEncoder instance.  Unused because all methods are static. */
  private BEncoder() {

  }

  /** Bencodes the given object.
   *
   * @param object the given object
   * @return the encoded bytes
   */
  public static byte[] bencode(final Object object) {
    try {
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      bencode(object, byteArrayOutputStream);
      return byteArrayOutputStream.toByteArray();
    } catch (IOException ioe) {
      throw new InternalError(ioe.toString());    // NOPMD
    }
  }

  /** Bencodes the given object onto the given output stream.
   *
   * @param obj the given object
   * @param outputStream the given output stream
   * @throws IOException when an input/output error occurs
   */
  @SuppressWarnings("unchecked")
  public static void bencode(final Object obj, final OutputStream outputStream) throws IOException {
    if (obj instanceof String) {
      bencode((String) obj, outputStream);
    } else if (obj instanceof byte[]) {
      bencode((byte[]) obj, outputStream);
    } else if (obj instanceof Number) {
      bencode((Number) obj, outputStream);
    } else if (obj instanceof List<?>) {
      bencode((List<?>) obj, outputStream);
    } else if (obj instanceof Map<?, ?>) {
      bencode((Map<Object, Object>) obj, outputStream);
    } else {
      throw new IllegalArgumentException("Cannot bencode: " + obj.getClass());
    }
  }

  /** Bencodes the given string.
   *
   * @param string the given string
   * @return the encoded bytes
   */
  public static byte[] bencode(final String string) {
    try {
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      bencode(string, byteArrayOutputStream);
      return byteArrayOutputStream.toByteArray();
    } catch (IOException ioe) {
      throw new InternalError(ioe.toString());    // NOPMD
    }
  }

  /** Bencodes the given string onto the given output stream.
   *
   * @param string the given string
   * @param outputStream the given output stream
   * @throws IOException when an input/output error occurs
   */
  public static void bencode(final String string, final OutputStream outputStream) throws IOException {
    bencode(string.getBytes("UTF-8"), outputStream);
  }

  /** Bencodes the given number.
   *
   * @param number the given number
   * @return the encoded bytes
   */
  public static byte[] bencode(final Number number) {
    try {
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      bencode(number, byteArrayOutputStream);
      return byteArrayOutputStream.toByteArray();
    } catch (IOException ioe) {
      throw new InternalError(ioe.toString());    // NOPMD
    }
  }

  /** Bencodes the given number onto the given output stream.
   *
   * @param number the given number
   * @param outputStream the given output stream
   * @throws IOException when an input/output error occurs
   */
  public static void bencode(final Number number, final OutputStream outputStream) throws IOException {
    outputStream.write('i');
    outputStream.write(number.toString().getBytes("UTF-8"));
    outputStream.write('e');
  }

  /** Bencodes the given list.
   *
   * @param list the given list
   * @return the encoded bytes
   */
  public static byte[] bencode(final List<?> list) {
    try {
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      bencode(list, byteArrayOutputStream);
      return byteArrayOutputStream.toByteArray();
    } catch (IOException ioe) {
      throw new InternalError(ioe.toString());    // NOPMD
    }
  }

  /** Bencodes the given list onto the given output stream.
   *
   * @param list the given list
   * @param outputStream the given output stream
   * @throws IOException when an input/output error occurs
   */
  public static void bencode(final List<?> list, final OutputStream outputStream) throws IOException {
    outputStream.write('l');
    final Iterator<?> list_iter = list.iterator();
    while (list_iter.hasNext()) {
      bencode(list_iter.next(), outputStream);
    }
    outputStream.write('e');
  }

  /** Bencodes the given bytes.
   *
   * @param bytes the bytes supplied
   * @return the encoded bytes
   */
  public static byte[] bencode(final byte[] bytes) {
    try {
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      bencode(bytes, byteArrayOutputStream);
      return byteArrayOutputStream.toByteArray();
    } catch (IOException ioe) {
      throw new InternalError(ioe.toString());   // NOPMD
    }
  }

  /** Bencodes the given bytes onto the given output stream.
   *
   * @param bytes the given bytes
   * @param outputStream the given output stream
   * @throws IOException when an input/output error occurs
   */
  public static void bencode(final byte[] bytes, final OutputStream outputStream) throws IOException {
    outputStream.write(Integer.toString(bytes.length).getBytes("UTF-8"));
    outputStream.write(':');
    outputStream.write(bytes);
  }

  /** Bencodes the given map.
   *
   * @param map the given map
   * @return the encoded bytes
   */
  public static byte[] bencode(final Map<Object, Object> map) {
    try {
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      bencode(map, byteArrayOutputStream);
      return byteArrayOutputStream.toByteArray();
    } catch (IOException ioe) {
      throw new InternalError(ioe.toString());    // NOPMD
    }
  }

  /** Bencodes the given map onto the given output stream.
   *
   * @param map the given map
   * @param outputStream the given output stream
   * @throws IOException when an input/output error occurs
   */
  public static void bencode(final Map<Object, Object> map, final OutputStream outputStream)
          throws IOException {
    outputStream.write('d');

    // if the keys are all strings then sort them
    boolean areKeysAllStrings = true;
    for (final Object key : map.keySet()) {
      if (!(key instanceof String)) {
        areKeysAllStrings = false;
        break;
      }
    }
    final List<Object> keys = new ArrayList<>(map.keySet());
    if (areKeysAllStrings) {
      final List<String> stringKeys = new ArrayList<>(keys.size());
      for (final Object key : keys) {
        stringKeys.add((String) key);
      }
      Collections.sort(stringKeys);
      keys.clear();
      keys.addAll(stringKeys);
    }

    for (final Object key : keys) {
      final Object value = map.get(key);
      bencode(key, outputStream);
      bencode(value, outputStream);
    }
    outputStream.write('e');
  }
}
