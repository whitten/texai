/**
 * A Base64 Encoder/Decoder.
 *
 * <p>
 * This class is used to encode and decode data in Base64 format as described in RFC 1521.
 *
 * <p>
 * This is "Open Source" software and released under the <a href="http://www.gnu.org/licenses/lgpl.html">GNU/LGPL</a> license.<br>
 * It is provided "as is" without warranty of any kind.<br>
 * Copyright 2003: Christian d'Heureuse, Inventec Informatik AG, Switzerland.<br>
 * Home page: <a href="http://www.source-code.biz">www.source-code.biz</a><br>
 *
 * <p>
 * Version history:<br>
 * 2003-07-22 Christian d'Heureuse (chdh): Module created.<br>
 * 2005-08-11 chdh: License changed from GPL to LGPL.<br>
 * 2006-11-21 chdh:<br>
 * &nbsp; Method encode(String) renamed to encodeString(String).<br>
 * &nbsp; Method decode(String) renamed to decodeString(String).<br>
 * &nbsp; New method encode(byte[],int) added.<br>
 * &nbsp; New method decode(String) added.<br>
 */
package org.texai.util;

/**
 * This class is used to encode and decode data in Base64 format as described in RFC 1521.
 */
@SuppressWarnings("PMD")
public final class Base64Coder {

  // the mapping table from 6-bit nibbles to Base64 characters
  private static final char[] NIBBLE_MAP = new char[64];
  // the mapping table from Base64 characters to 6-bit nibbles
  private static final byte[] BASE64_MAP = new byte[128];

  static {
    int i = 0;
    for (char c = 'A'; c <= 'Z'; c++) {
      NIBBLE_MAP[i++] = c;
    }
    for (char c = 'a'; c <= 'z'; c++) {
      NIBBLE_MAP[i++] = c;
    }
    for (char c = '0'; c <= '9'; c++) {
      NIBBLE_MAP[i++] = c;
    }
    NIBBLE_MAP[i++] = '+';
    NIBBLE_MAP[i++] = '/';
  }

  // Initialize the mapping table from Base64 characters to 6-bit nibbles.
  static {
    for (int i = 0; i < BASE64_MAP.length; i++) {
      BASE64_MAP[i] = -1;
    }
    for (int i = 0; i < 64; i++) {
      BASE64_MAP[NIBBLE_MAP[i]] = (byte) i;
    }
  }

  /**
   * Private constructor that prevents class instantiation.
   */
  private Base64Coder() {
  }

  /**
   * Decodes the given string.
   *
   * @param s the given string
   *
   * @return the decoded string
   */
  public static String decodeEntities(final String s) {
    final StringBuilder stringBuilder = new StringBuilder();
    int i = 0;
    final int s_len = s.length();
    while (i < s_len) {
      final char c = s.charAt(i++);
      if (c == '%') {
        final char c1 = s.charAt(i++);
        final char c2 = s.charAt(i++);
        final char decodedChar = (char) (16 * ByteUtils.fromHex(c1) + ByteUtils.fromHex(c2));
        stringBuilder.append(Character.toString(decodedChar));
      } else {
        stringBuilder.append(c);
      }
    }
    return stringBuilder.toString();
  }

  /**
   * Encodes the given string.
   *
   * @param s a String to be encoded.
   *
   * @return A String with the Base64 encoded data.
   */
  public static String encodeString(final String s) {
    return new String(encode(s.getBytes()));
  }

  /**
   * Encodes the given byte array.
   *
   * @param in an array containing the data bytes to be encoded.
   *
   * @return A character array with the Base64 encoded data.
   */
  public static char[] encode(final byte[] in) {
    return encode(in, in.length);
  }

  /**
   * Encodes the given number of bytes in the given byte array.
   *
   * @param in an array containing the data bytes to be encoded.
   * @param iLen number of bytes to process in <code>in</code>.
   *
   * @return A character array with the Base64 encoded data.
   */
  public static char[] encode(final byte[] in, final int iLen) {
    int oDataLen = (iLen * 4 + 2) / 3;       // output length without padding
    int oLen = ((iLen + 2) / 3) * 4;         // output length including padding
    char[] out = new char[oLen];
    int ip = 0;
    int op = 0;
    while (ip < iLen) {
      int i0 = in[ip++] & 0xff;
      int i1 = ip < iLen ? in[ip++] & 0xff : 0;
      int i2 = ip < iLen ? in[ip++] & 0xff : 0;
      int o0 = i0 >>> 2;
      int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
      int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
      int o3 = i2 & 0x3F;
      out[op++] = NIBBLE_MAP[o0];
      out[op++] = NIBBLE_MAP[o1];
      out[op] = op < oDataLen ? NIBBLE_MAP[o2] : '=';
      op++;
      out[op] = op < oDataLen ? NIBBLE_MAP[o3] : '=';
      op++;
    }
    return out;
  }

  /**
   * Decodes the given base 64 string into a string.
   *
   * @param s a Base64 String to be decoded.
   *
   * @return A String containing the decoded data.
   */
  public static String decodeString(final String s) {
    return new String(decode(s));
  }

  /**
   * Decodes the given base 64 string into a byte array.
   *
   * @param s a Base64 String to be decoded.
   *
   * @return An array containing the decoded data bytes.
   */
  public static byte[] decode(final String s) {
    return decode(s.toCharArray());
  }

  /**
   * Decodes the given character array into a byte array. No blanks or line breaks are allowed within the Base64 encoded data.
   *
   * @param in a character array containing the Base64 encoded data.
   *
   * @return An array containing the decoded data bytes.
   */
  public static byte[] decode(final char[] in) {
    int iLen = in.length;
    if (iLen % 4 != 0) {
      throw new IllegalArgumentException("Length of Base64 encoded input string is not a multiple of 4.");
    }
    while (iLen > 0 && in[iLen - 1] == '=') {
      iLen--;
    }
    int oLen = (iLen * 3) / 4;
    byte[] out = new byte[oLen];
    int ip = 0;
    int op = 0;
    while (ip < iLen) {
      int i0 = in[ip++];
      int i1 = in[ip++];
      int i2 = ip < iLen ? in[ip++] : 'A';
      int i3 = ip < iLen ? in[ip++] : 'A';
      if (i0 > 127 || i1 > 127 || i2 > 127 || i3 > 127) {
        throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
      }
      int b0 = BASE64_MAP[i0];
      int b1 = BASE64_MAP[i1];
      int b2 = BASE64_MAP[i2];
      int b3 = BASE64_MAP[i3];
      if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0) {
        throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
      }
      int o0 = (b0 << 2) | (b1 >>> 4);
      int o1 = ((b1 & 0xf) << 4) | (b2 >>> 2);
      int o2 = ((b2 & 3) << 6) | b3;
      out[op++] = (byte) o0;
      if (op < oLen) {
        out[op++] = (byte) o1;
      }
      if (op < oLen) {
        out[op++] = (byte) o2;
      }
    }
    return out;
  }

} // end class Base64Coder

