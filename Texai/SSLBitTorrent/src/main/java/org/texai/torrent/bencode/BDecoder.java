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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Decodes a bencoded stream to <code>BEValue</code>s.
 *
 * A bencoded byte stream can represent byte arrays, numbers, lists and maps
 * (dictionaries).
 *
 * It currently contains a hack to indicate a name of a dictionary of which a
 * SHA-1 digest hash should be calculated (the hash over the original bencoded
 * bytes).
 *
 * @author Mark Wielaard (mark@klomp.org).
 */
public class BDecoder {

  /** the InputStream to BDecode */
  private final InputStream inputStream;
  /** the last indicator read.
   * Zero if unknown.
   * '0'..'9' indicates a byte[].
   * 'i' indicates an Number.
   * 'l' indicates a List.
   * 'd' indicates a Map.
   * 'e' indicates end of Number, List or Map (only used internally).
   * -1 indicates end of stream.
   * Call getNextIndicator to get the current value (will never return zero).
   */
  private int lastIndicator = 0;
  /** the special map which is used for ugly hack to get SHA hash over the metainfo info map */
  private String specialMap = "info";
  /** the indicator whether we are using the special map */
  private boolean isSpecialMap = false;
  /** the message digest */
  private final MessageDigest shaDigest;

  /** Return the SHA has over bytes that make up the special map.
   *
   * @return the SHA has over bytes that make up the special map
   */
  public byte[] get_special_map_digest() {
    return shaDigest.digest();
  }

  /** Sets the special map name.  Name defaults to "info".
   *
   * @param name the special map name
   */
  public void set_special_map_name(final String name) {
    specialMap = name;
  }

  /** Initalizes a new BDecoder. Nothing is read from the given
   * <code>InputStream</code> yet.
   *
   * @param inputStream the input stream
   */
  public BDecoder(final InputStream inputStream) {
    this.inputStream = inputStream;
    // XXX - Used for ugly hack.
    try {
      shaDigest = MessageDigest.getInstance("SHA");
    } catch (NoSuchAlgorithmException nsa) {
      throw new InternalError(nsa.toString());    // NOPMD
    }
  }

  /** Creates a new BDecoder and immediately decodes the first value it sees.
   *
   * @param inputStream the input stream
   * @return the first BEValue on the stream or null when the stream has ended
   *
   * @exception IOException when somthing bad happens with the stream to read from.
   */
  public static BEValue bdecode(final InputStream inputStream) throws IOException {
    return new BDecoder(inputStream).bdecode();
  }

  /** Returns what the next bencoded object will be on the stream or -1 when
   * the end of stream has been reached. Can return something unexpected (not
   * '0' .. '9', 'i', 'l' or 'd') when the stream isn't bencoded.
   *
   * This might or might not read one extra byte from the stream.
   *
   * @return what the next bencoded object will be on the stream or -1 when
   * the end of stream has been reached
   * @throws IOException when an input/output error occurs
   */
  public int getNextIndicator() throws IOException {
    if (lastIndicator == 0) {
      lastIndicator = inputStream.read();
      // XXX - Used for ugly hack
      if (isSpecialMap) {
        shaDigest.update((byte) lastIndicator);
      }
    }
    return lastIndicator;
  }

  /** Gets the next indicator and returns either null when the stream has ended
   * or bdecodes the rest of the stream and returns the appropriate BEValue
   * encoded object.
   *
   * @return the appropriate BEValue encoded object or null when the stream has ended
   * @throws IOException when an input/output error occurs
   */
  public BEValue bdecode() throws IOException {
    lastIndicator = getNextIndicator();
    if (lastIndicator == -1) {
      return null;
    }

    if (lastIndicator >= '0' && lastIndicator <= '9') {
      return bdecodeBytes();
    } else if (lastIndicator == 'i') {
      return bdecodeNumber();
    } else if (lastIndicator == 'l') {
      return bdecodeList();
    } else if (lastIndicator == 'd') {
      return bdecodeMap();
    } else {
      throw new InvalidBEncodingException("Unknown indicator '" + lastIndicator + "'");
    }
  }

  /** Returns the next bencoded value on the stream and makes sure it is a byte
   * array. If it is not a bencoded byte array it will throw
   * InvalidBEncodingException.
   *
   * @return the next bencoded value on the stream
   * @throws IOException when the next bencoded value is not a byte array
   */
  public BEValue bdecodeBytes() throws IOException {
    int indicator = getNextIndicator();
    int num = indicator - '0';
    if (num < 0 || num > 9) {
      throw new InvalidBEncodingException("Number expected, not '" + (char) indicator + "'");
    }
    lastIndicator = 0;

    indicator = read();
    int index = indicator - '0';
    while (index >= 0 && index <= 9) {
      // XXX - This can overflow!
      num = num * 10 + index;
      indicator = read();
      index = indicator - '0';
    }

    if (indicator != ':') {
      throw new InvalidBEncodingException("Colon expected, not '" + (char) indicator + "'");
    }

    return new BEValue(read(num));
  }

  /** Returns the next bencoded value on the stream and makes sure it is a
   * number. If it is not a number it will throw InvalidBEncodingException.
   *
   * @return the next bencoded value on the stream
   * @throws IOException when the next bencoded value on the stream is not a number
   */
  public BEValue bdecodeNumber() throws IOException {
    int indicator = getNextIndicator();
    if (indicator != 'i') {
      throw new InvalidBEncodingException("Expected 'i', not '" + (char) indicator + "'");
    }
    lastIndicator = 0;

    indicator = read();
    if (indicator == '0') {
      indicator = read();
      if (indicator == 'e') {
        return new BEValue(BigInteger.ZERO);
      } else {
        throw new InvalidBEncodingException("'e' expected after zero," + " not '" + (char) indicator + "'");
      }
    }

    // XXX - We don't support more the 255 char big integers
    final char[] chars = new char[256];
    int off = 0;

    if (indicator == '-') {
      indicator = read();
      if (indicator == '0') {
        throw new InvalidBEncodingException("Negative zero not allowed");
      }
      chars[off] = (char) indicator;
      off++;
    }

    if (indicator < '1' || indicator > '9') {
      throw new InvalidBEncodingException("Invalid Integer start '" + (char) indicator + "'");
    }
    chars[off] = (char) indicator;
    off++;

    indicator = read();
    int index = indicator - '0';
    while (index >= 0 && index <= 9) {
      chars[off] = (char) indicator;
      off++;
      indicator = read();
      index = indicator - '0';
    }

    if (indicator != 'e') {
      throw new InvalidBEncodingException("Integer should end with 'e'");
    }

    return new BEValue(new BigInteger(new String(chars, 0, off)));
  }

  /** Returns the next bencoded value on the stream and makes sure it is a
   * list. If it is not a list it will throw InvalidBEncodingException.
   *
   * @return the next bencoded value on the stream
   * @throws IOException when the next bencoded value on the stream is not a list
   */
  public BEValue bdecodeList() throws IOException {
    int indicator = getNextIndicator();
    if (indicator != 'l') {
      throw new InvalidBEncodingException("Expected 'l', not '" + (char) indicator + "'");
    }
    lastIndicator = 0;

    final List<BEValue> result = new ArrayList<>();
    indicator = getNextIndicator();
    while (indicator != 'e') {
      result.add(bdecode());
      indicator = getNextIndicator();
    }
    lastIndicator = 0;

    return new BEValue(result);
  }

  /** Returns the next bencoded value on the stream and makes sure it is a map
   * (dictonary). If it is not a map it will throw InvalidBEncodingException.
   *
   * @return the next bencoded value on the stream
   * @throws IOException when the next bencoded value on the stream is not a map (String --> BEValue)
   */
  public BEValue bdecodeMap() throws IOException {
    int indicator = getNextIndicator();
    if (indicator != 'd') {
      throw new InvalidBEncodingException("Expected 'd', not '" + (char) indicator + "'");
    }
    lastIndicator = 0;

    final Map<String, BEValue> result = new HashMap<>();
    indicator = getNextIndicator();
    while (indicator != 'e') {
      // Dictonary keys are always strings.
      final String key = bdecode().getString();

      // XXX ugly hack
      final boolean special = specialMap.equals(key);
      if (special) {
        isSpecialMap = true;
      }

      final BEValue value = bdecode();
      result.put(key, value);

      // XXX ugly hack continued
      if (special) {
        isSpecialMap = false;
      }

      indicator = getNextIndicator();
    }
    lastIndicator = 0;

    return new BEValue(result);
  }

  /** Returns the next byte read from the InputStream (as int). Throws
   * EOFException if InputStream.read() returned -1.
   *
   * @return the next byte read
   * @throws IOException when an input/output error occurs
   */
  private int read() throws IOException {
    final int byte1 = inputStream.read();
    if (byte1 == -1) {
      throw new EOFException();
    }
    if (isSpecialMap) {
      shaDigest.update((byte) byte1);
    }
    return byte1;
  }

  /** Returns a byte[] containing length valid bytes starting at offset zero.
   * Throws EOFException if InputStream.read() returned -1 before all
   * requested bytes could be read. Note that the byte[] returned might be
   * bigger then requested but will only contain length valid bytes. The
   * returned byte[] will be reused when this method is called again.
   *
   * @param length the length of the bytes to be read
   * @return a byte[] containing length valid bytes starting at offset zero
   * @throws IOException when an input/output error occurs
   */
  private byte[] read(final int length) throws IOException {
    final byte[] result = new byte[length];

    int read = 0;
    while (read < length) {
      final int index = inputStream.read(result, read, length - read);
      if (index == -1) {
        throw new EOFException();
      }
      read += index;
    }

    if (isSpecialMap) {
      shaDigest.update(result, 0, length);
    }
    return result;
  }
}
