/*
 * StringUtils.java
 *
 * Created on October 4, 2006, 2:36 PM
 *
 * Description:
 *
 * Copyright (C) 2006 Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author reed
 */
public final class StringUtils {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(StringUtils.class);
  /** the hex digits */
  private static final String HEX_DIGITS = "0123456789abcdef";

  /** Creates a new instance of StringUtils. */
  private StringUtils() {
  }

  /** Returns a sorted list of the strings corresponding to the given collection of objects.
   *
   * @param objects the given collection of objects
   * @return a sorted list of the strings
   */
  public static List<String> toSortedStrings(final Collection<? extends Object> objects) {
    //Preconditions
    assert objects != null : "objects must not be null";

    final List<String> strings = new ArrayList<>();
    for (final Object obj : objects) {
      strings.add(obj.toString());
    }
    Collections.sort(strings);
    return strings;
  }

  /** Returns a string representation of the given float array.
   *
   * @param floatArray the given float array
   * @return a string representation of the given float array
   */
  public static String floatArrayToString(final float[] floatArray) {
    final StringBuilder stringBuilder = new StringBuilder();
    boolean isFirst = true;
    stringBuilder.append('[');
    for (int i = 0; i < floatArray.length; i++) {
      if (isFirst) {
        isFirst = false;
      } else {
        stringBuilder.append(", ");
      }
      stringBuilder.append(floatArray[i]);
    }
    stringBuilder.append(']');

    return stringBuilder.toString();
  }

  /** Returns a bit-string representation of the given boolean array.
   *
   * @param booleanArray the given boolean array
   * @return a bit-string representation of the given boolean array
   */
  public static String booleanArrayToBitString(final boolean[] booleanArray) {
    final StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < booleanArray.length; i++) {
      if (booleanArray[i]) {
        stringBuilder.append('1');
      } else {
        stringBuilder.append('0');
      }
    }
    return stringBuilder.toString();
  }

  /** Returns whether the given string is a valid Java class name.
   *
   * @param string the given string
   * @return whether the given string is a valid Java class name
   */
  public static boolean isJavaClassName(final String string) {
    if (string == null
            || string.isEmpty()
            || !Character.isJavaIdentifierStart(string.charAt(0))
            || string.contains("..")) {
      return false;
    }
    for (int i = 1; i < string.length(); i++) {
      final char ch = string.charAt(i);
      if (!Character.isJavaIdentifierPart(ch)
              && ch != '.') {
        return false;
      }
    }
    return true;
  }

  /** Returns whether the given string is a non-empty string.
   *
   * @param string the given string
   * @return whether the given string is a non-empty string
   */
  public static boolean isNonEmptyString(final String string) {
    return string != null && !string.isEmpty();
  }

  /** Splits the given string on spaces, which is faster than String.split(...) for this special case.
   *
   * @param string the given string
   * @return the words that compose the string
   */
  public static List<String> splitOnSpace(final String string) {
    final List<String> words = new ArrayList<>();
    final int string_len = string.length();
    int index = 0;
    for (int i = 0; i < string_len; i++) {
      final char ch = string.charAt(i);
      if (ch == ' ') {
        if (i > index) {
          words.add(string.substring(index, i));
        }
        index = i + 1;
      }
    }
    if (index < string_len) {
      words.add(string.substring(index));
    }
    return words;
  }

  /** Reverses a string which consists of comma-separated items.
   *  "a, b, c" --> "c, b, a"
   *
   * @param string the given string
   * @return the string with the comma-separated items reversed
   */
  public static String reverseCommaDelimitedString(final String string) {
    final List<String> items = new ArrayList<>();
    final int string_len = string.length();
    int index = 0;
    for (int i = 0; i < string_len; i++) {
      final char ch = string.charAt(i);
      if (ch == ',') {
        if (i > index) {
          items.add(string.substring(index, i).trim());
        }
        index = i + 1;
      }
    }
    if (index < string_len) {
      items.add(string.substring(index).trim());
    }

    final StringBuilder stringBuilder = new StringBuilder();
    boolean isFirst = true;
    for (int i = items.size() - 1; i >= 0; i--) {
      if (isFirst) {
        isFirst = false;
      } else {
        stringBuilder.append(", ");
      }
      stringBuilder.append(items.get(i));
    }
    return stringBuilder.toString();
  }

  /** Splits the given string on spaces and certain embedded HTML tags.
   *
   * @param string the given string
   * @return the words and HTML tags that compose the string
   */
  public static List<String> splitHTMLTags(final String string) {
    if (!string.contains("<")) {
      return splitOnSpace(string);
    }
    String tempString = string;
    tempString = replace(tempString, "<br>", " <br> ");
    tempString = replace(tempString, "<ol>", " <ol> ");
    tempString = replace(tempString, "</ol>", " </ol> ");
    tempString = replace(tempString, "<ul>", " <ul> ");
    tempString = replace(tempString, "</ul>", " </ul> ");
    tempString = replace(tempString, "<li>", " <li> ");
    tempString = replace(tempString, "</li>", " </li> ");
    tempString = replace(tempString, "<strong>", " <strong> ");
    tempString = replace(tempString, "</strong>", " </strong> ");
    return splitOnSpace(tempString.trim());
  }

  /** Logs the character differences between two given equal length strings.
   *
   * @param string1 the first given string
   * @param string2 the second given string
   */
  public static void logStringCharacterDifferences(final String string1, final String string2) {
    //Preconditions
    assert string1 != null : "string1 must not be null";
    assert string2 != null : "string1 must not be null";
    assert !string1.isEmpty() : "string1 must not be empty";
    assert string1.length() == string2.length() : "string1 " + string1.length() + " and string2 " + string2.length() + " must have the same length";

    LOGGER.info("comparing '" + string1 + "' and '" + string2 + "'...");
    for (int i = 0; i < string1.length(); i++) {
      final char ch1 = string1.charAt(i);
      final char ch2 = string2.charAt(i);
      final String message;
      if (ch1 == ch2) {
        message = "";
      } else {
        message = " not equal";
      }
      LOGGER.info("  '" + ch1 + "' - '" + ch2 + "'" + message);
    }
  }

  /** Escapes embedded single quote and backslash characters in the given string.
   *
   * @param string the given string
   * @return the given string with embedded single quote characters
   * preceded by a backslash character, and with embedded backslash
   * characters preceded by another (escaping) backslash character
   */
  public static String escapeSingleQuotes(final String string) {
    if (string == null) {
      return null;
    }
    final String result = string.replaceAll("\\\\", "\\\\\\\\");
    return result.replaceAll("'", "\\\\'");
  }

  /** Gets the given throwable stack trace as a string.
   *
   * @param throwable the throwable
   * @return the given throwable stack trace as a string
   */
  public static String getStackTraceAsString(final Throwable throwable) {
    //Preconditions
    assert throwable != null : "ex must not be null";

    final StringWriter stringWriter = new StringWriter();
    throwable.printStackTrace(new PrintWriter(stringWriter));
    return stringWriter.toString();
  }

  /** Removes any enclosing double quotes from the given string.
   *
   * @param string the given string
   * @return the given string after removing any enclosing double quotes
   */
  public static String removeEnclosingDoubleQuotes(final String string) {
    //Preconditions
    assert string != null : "string must not be null";

    String resultString = string;
    if (resultString.startsWith("\"")) {
      resultString = resultString.substring(1);
    }
    if (resultString.endsWith("\"")) {
      resultString = resultString.substring(0, resultString.length() - 1);
    }
    return resultString;
  }

  /** Appends the given number of spaces to the given string builder.
   *
   * @param stringBuilder the given string builder
   * @param indent the given number of spaces
   */
  public static void indentSpaces(final StringBuilder stringBuilder, final int indent) {
    //Preconditions
    assert stringBuilder != null : "stringBuilder must not be null";
    assert indent >= 0 : "indent must not be negative";

    for (int i = 0; i < indent; i++) {
      stringBuilder.append(' ');
    }
  }

  /** Appends the given number of spaces to the given buffered writer.
   *
   * @param bufferedWriter the given buffered writer
   * @param indent the given number of spaces
   */
  public static void indentSpaces(final BufferedWriter bufferedWriter, final int indent) {
    //Preconditions
    assert bufferedWriter != null : "bufferedWriter must not be null";
    assert indent >= 0 : "indent must not be negative";

    try {
      for (int i = 0; i < indent; i++) {
        bufferedWriter.write(' ');
      }
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Returns the contents of the given input stream as a string.
   *
   * @param inputStream the given input stream
   * @return the contents of the given input stream as a string
   */
  public static String convertInputStreamToString(final InputStream inputStream) {
    final StringBuilder stringBuilder = new StringBuilder();
    final byte[] buffer = new byte[4096];
    try {
      while (true) {
        final int nbrChars = inputStream.read(buffer);
        if (nbrChars == -1) {
          break;
        }
        stringBuilder.append(new String(buffer, 0, nbrChars));
      }
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
    return stringBuilder.toString();
  }

  /** Returns a string of length len made up of blanks.
   *
   * @param length the length of the output String.
   * @return the string of blanks.
   */
  public static String makeBlankString(final int length) {
    //Preconditions
    assert length >= 0 : "length must not be negative";

    final char[] buffer = new char[length];
    for (int i = 0; i != buffer.length; i++) {
      buffer[i] = ' ';
    }
    return new String(buffer);
  }

  /** Returns given byte array as a hex string.
   *
   * @param buffer the buffer containing the bytes to be converted to hex.
   * @param length the number of bytes to be converted
   * @return a hex representation of the bytes.
   */
  public static String toHex(final byte[] buffer, final int length) {
    final StringBuffer stringBuffer = new StringBuffer();

    for (int i = 0; i != length; i++) {
      final int byteValue = buffer[i] & 0xff;
      stringBuffer.append(HEX_DIGITS.charAt(byteValue >> 4));
      stringBuffer.append(HEX_DIGITS.charAt(byteValue & 0xf));
    }

    return stringBuffer.toString();
  }

  /** Returns the given byte array as a hex string.
   *
   * @param buffer the bytes to be converted.
   * @return a hex representation of data.
   */
  public static String toHex(final byte[] buffer) {
    return toHex(buffer, buffer.length);
  }

  /** Returns the package name from the fully qualified class name.
   *
   * @param className the fully qualified class name
   * @return the package name
   */
  public static String packageFromClassName(final String className) {
    //Preconditions
    assert className != null : "className must not be null";
    assert !className.isEmpty() : "className must not be empty";

    final int index = className.lastIndexOf('.');
    assert index > -1 : "cannot find package in import " + className;
    return className.substring(0, index);
  }

  /** Returns the simple class name from the fully qualified class name.
   *
   * @param className the fully qualified class name
   * @return the package name
   */
  public static String simpleClassName(final String className) {
    //Preconditions
    assert className != null : "className must not be null";
    assert !className.isEmpty() : "className must not be empty";

    final int index = className.lastIndexOf('.');
    assert index > -1 : "cannot find package in import " + className;
    return className.substring(index + 1);
  }

  /** Gets a lower case predicate or variable name derived from the given simple class name.
   *
   * @param simpleClassName the given simple class name
   * @return a lower case predicate or variable name derived from the given simple class name
   */
  public static String getLowerCasePredicateName(final String simpleClassName) {
    //Preconditions
    assert simpleClassName != null : "simpleClassName must not be null";
    assert !simpleClassName.isEmpty() : "simpleClassName must not be empty";

    // predicates begin with a lower case letter, but class names are capitalized and may also begin with a accronym
    if (simpleClassName.length() == 1) {
      // trivial case
      return simpleClassName.toLowerCase();
    } else {
      final StringBuilder stringBuilder = new StringBuilder();

      int index = 0;
      final int simpleClassName_len = simpleClassName.length();
      while (true) {
        if (index >= simpleClassName_len) {
          // reached the end with all upper case characters
          break;
        }
        char ch = simpleClassName.charAt(index);
        stringBuilder.append(Character.toLowerCase(ch));

        if (index < simpleClassName_len - 2
                && Character.isUpperCase(simpleClassName.charAt(index + 1))
                && Character.isLowerCase(simpleClassName.charAt(index + 2))) {
          break;
        } else if (index < simpleClassName_len - 1
                && Character.isLowerCase(simpleClassName.charAt(index + 1))) {
          break;
        }
        index++;
      }

      // copy any remaining characters unchanged
      for (int i = index + 1; i < simpleClassName_len; i++) {
        stringBuilder.append(simpleClassName.charAt(i));
      }
      return stringBuilder.toString();
    }
  }

  /** Ensures that the given string builder has one newline characters at the end of its buffer.
   *
   * @param stringBuilder the given string builder
   */
  public static void ensureOneNewLine(final StringBuilder stringBuilder) {
    //Preconditions
    assert stringBuilder != null : "stringBuilder must not be null";

    final int length = stringBuilder.length();
    if (length == 0) {
      stringBuilder.append('\n');
      return;
    }
    final char ch1 = stringBuilder.charAt(length - 1);
    if (ch1 == '\n') {
      return;
    } else {
      stringBuilder.append('\n');
      return;
    }
  }

  /** Ensures that the given string builder has two newline characters at the end of its buffer.
   *
   * @param stringBuilder the given string builder
   */
  public static void ensureTwoNewLines(final StringBuilder stringBuilder) {
    //Preconditions
    assert stringBuilder != null : "stringBuilder must not be null";

    final int length = stringBuilder.length();
    if (length == 0) {
      stringBuilder.append('\n');
      stringBuilder.append('\n');
      return;
    }
    final char ch1 = stringBuilder.charAt(length - 1);
    if (length == 1) {
      stringBuilder.append('\n');
      if (ch1 == '\n') {
        return;
      } else {
        stringBuilder.append('\n');
        return;
      }
    }
    final char ch2 = stringBuilder.charAt(length - 2);
    if (ch1 == '\n') {
      if (ch2 == '\n') {
        return;
      } else {
        stringBuilder.append('\n');
        return;
      }
    } else {
      stringBuilder.append('\n');
      stringBuilder.append('\n');
      return;
    }
  }

  /** Returns the single string formed by concatenating the given strings, separating them by a space.
   *
   * @param strings the given strings
   * @return the single string formed by concatenating the given strings, separating them by a space
   */
  public static String toSingleString(final List<String> strings) {
    //Preconditions
    assert strings != null : "strings must not be null";

    final StringBuilder stringBuilder = new StringBuilder();
    boolean isFirst = true;
    for (final String string : strings) {
      if (isFirst) {
        isFirst = false;
      } else {
        stringBuilder.append(' ');
      }
      stringBuilder.append(string);
    }
    return stringBuilder.toString();
  }

  /** Returns the given string padded with spaces if required to have the given length.
   *
   * @param string the given string
   * @param length the given length
   * @return the given string padded with spaces if required to have the given length
   */
  public static String padWithTrailingSpaces(final String string, final int length) {
    //Preconditions
    assert string != null : "string must not be null";

    final int string_len = string.length();
    if (string_len >= length) {
      return string;
    }
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(string);
    final int nbrSpacesToPad = length - string_len;
    for (int i = 0; i < nbrSpacesToPad; i++) {
      stringBuilder.append(' ');
    }
    return stringBuilder.toString();
  }

  /** Returns the given string after removing any numeric suffix.
   *
   * @param string the given string
   * @return the given string after removing any numeric suffix
   */
  public static String stripNumericSuffix(final String string) {
    //Preconditions
    assert string != null : "string must not be null";

    if (string.isEmpty()) {
      return string;
    }
    int index = string.length() - 1;
    while (index >= 0) {
      if (Character.isDigit(string.charAt(index))) {
        index--;
      } else {
        break;
      }
    }
    return string.substring(0, index + 1);
  }

  /** Returns whether two lists of strings are consistent with respect to order, while allowing missing items in the
   * second list, and allowing optional capitalization of the first word in the first list.
   *
   * @param strings1 the first list of strings
   * @param strings2 the second list of strings
   * @return
   */
  public static boolean isOrderConsistent(final List<String> strings1, final List<String> strings2) {
    //Preconditions
    assert strings1 != null : "strings1 must not be null";
    assert strings2 != null : "strings2 must not be null";

    if (strings1.size() < strings2.size()) {
      return false;
    } else if (strings1.isEmpty()) {
      return true;
    }
    int index1 = 0;
    int index2 = 0;
    while (true) {
      String string1 = strings1.get(index1);
      if (index2 >= strings2.size()) {
        // reached the end of the second list before finding the expected match
        return false;
      }
      final String string2 = strings2.get(index2);
      if (string1.equals(string2)
              || (index1 == 0 && StringUtils.uncapitalize(string1).equals(string2))) {
        index1++;
        index2++;
        if (index1 >= strings1.size()) {
          // reached the end of the first list, return whether the end of the second list has also been reached
          return index2 >= strings2.size();
        } else {
          continue;
        }
      }
      while (true) {
        index1++;
        if (index1 >= strings1.size()) {
          // reached the end of the first before finding the expected match
          return false;
        }
        string1 = strings1.get(index1);
        if (string1.equals(string2)) {
          // found expected match after possibly skipping items in the second list
          break;
        }
      }
    }
  }

  // Adapted from the Spring Framework org.springframework.util.SpringUtils having the Apache license.
  //---------------------------------------------------------------------
  // General convenience methods for working with Strings
  //---------------------------------------------------------------------
  /** Capitalizes a <code>String</code>, changing the first letter to
   * upper case as per {@link Character#toUpperCase(char)}.
   * No other letters are changed.
   * @param string the String to capitalize, may be <code>null</code>
   * @return the capitalized String, <code>null</code> if null
   */
  public static String capitalize(final String string) {
    return changeFirstCharacterCase(string, true);
  }

  /** Uncapitalizes a <code>String</code>, changing the first letter to
   * lower case as per {@link Character#toLowerCase(char)}.
   * No other letters are changed.
   * @param string the String to uncapitalize, may be <code>null</code>
   * @return the uncapitalized String, <code>null</code> if null
   */
  public static String uncapitalize(final String string) {
    return changeFirstCharacterCase(string, false);
  }

  /** Changes case of the first character of the string.
   *
   * @param string the given string
   * @param isCapitalized whether to capitalize the first character
   * @return the transformed string
   */
  private static String changeFirstCharacterCase(final String string, final boolean isCapitalized) {
    if (string == null || string.length() == 0) {
      return string;
    }
    final StringBuilder stringBuilder = new StringBuilder(string.length());
    if (isCapitalized) {
      stringBuilder.append(Character.toUpperCase(string.charAt(0)));
    } else {
      stringBuilder.append(Character.toLowerCase(string.charAt(0)));
    }
    stringBuilder.append(string.substring(1));
    return stringBuilder.toString();
  }

  /** Returns whether the given CharSequence is neither <code>null</code> nor length 0.
   * Note: Will return <code>true</code> for a CharSequence that purely consists of whitespace.
   * <p><pre>
   * StringUtils.hasLength(null) = false
   * StringUtils.hasLength("") = false
   * StringUtils.hasLength(" ") = true
   * StringUtils.hasLength("Hello") = true
   * </pre>
   * @param str the CharSequence to check (may be <code>null</code>)
   * @return <code>true</code> if the CharSequence is not null and has length
   * @see #hasText(String)
   */
  public static boolean hasLength(final CharSequence str) {
    return (str != null && str.length() > 0);
  }

  /** Returns whether the given String is neither <code>null</code> nor length 0.
   * Note: Will return <code>true</code> for a String that purely consists of whitespace.
   * @param str the String to check (may be <code>null</code>)
   * @return <code>true</code> if the String is not null and has length
   * @see #hasLength(CharSequence)
   */
  public static boolean hasLength(final String str) {
    return hasLength((CharSequence) str);
  }

  /** Replaces all occurrences of a substring within a string with
   * another string.
   * @param inString String to examine
   * @param oldPattern String to replace
   * @param newPattern String to insert
   * @return a String with the replacements
   */
  public static String replace(
          final String inString,
          final String oldPattern,
          final String newPattern) {
    //Preconditions
    assert inString != null : "inString must not be null";
    assert oldPattern != null : "oldPattern must not be null";
    assert newPattern != null : "newPattern must not be null";

    if (!hasLength(inString) || !hasLength(oldPattern)) {
      return inString;
    }
    final StringBuilder stringBuilder = new StringBuilder();
    int pos = 0; // our position in the old string
    int index = inString.indexOf(oldPattern);
    // the index of an occurrence we've found, or -1
    final int patLen = oldPattern.length();
    while (index >= 0) {
      stringBuilder.append(inString.substring(pos, index));
      stringBuilder.append(newPattern);
      pos = index + patLen;
      index = inString.indexOf(oldPattern, pos);
    }
    stringBuilder.append(inString.substring(pos));
    // remember to append any characters to the right of a match
    return stringBuilder.toString();
  }

  /** Counts the occurrences of the substring in the given string.
   * @param string the given string
   * @param substring the given substring
   *
   * @return the number of occurrences
   */
  public static int countOccurrencesOf(final String string, final String substring) {
    if (string == null || substring == null || string.length() == 0 || substring.length() == 0) {
      return 0;
    }
    int count = 0;
    int pos = 0;
    int idx;
    while ((idx = string.indexOf(substring, pos)) != -1) {
      ++count;
      pos = idx + substring.length();
    }
    return count;
  }

  /** Strips punctuation and digits from the given string.
   *
   * @param string the given string
   * @return a string consisting of the given string with punctuation and digits removed
   */
  public static String stripPunctuationAndDigits(final String string) {
    //Preconditions
    assert string != null : "string must not be null";

    final StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < string.length(); i++) {
      final char char1 = string.charAt(i);
      if (char1 == 45) {
        stringBuilder.append(' '); // replace dash with space
      } else if ((char1 >= 9 && char1 <= 13) || // whitespace
              char1 == 32 || // space
              (char1 >= 65 && string.charAt(i) <= 90) || // uppercase letters
              (char1 >= 97 && char1 <= 122)) { // lowercase letters
        stringBuilder.append(string.charAt(i));
      }
    }
    return stringBuilder.toString();
  }
  //---------------------------------------------------------------------
  // Adapted from the Spring Framework org.springframework.web.util.HtmlUtils having the Apache license.
  //---------------------------------------------------------------------
  /**
   * Shared instance of pre-parsed HTML character entity references.
   */
  private static final HtmlCharacterEntityReferences characterEntityReferences =
          new HtmlCharacterEntityReferences();

  /** Turns special characters into HTML character references.
   * Handles complete character set defined in HTML 4.01 recommendation.  Also changes newlines to HTML break tags,
   * and considers all spaces to be non-break spaces.
   * <p>Escapes all special characters to their corresponding
   * entity reference (e.g. <code>&lt;</code>).
   * <p>Reference:
   * <a href="http://www.w3.org/TR/html4/sgml/entities.html">
   * http://www.w3.org/TR/html4/sgml/entities.html
   * </a>
   * @param input the (unescaped) input string
   * @return the escaped string
   */
  public static String htmlEscape(final String input) {
    if (input == null) {
      return null;
    }
    final StringBuilder escaped = new StringBuilder(input.length() * 2);
    for (int i = 0; i < input.length(); i++) {
      char character = input.charAt(i);
      if (character == '\n') {
        escaped.append("<br>");
      } else {
        final String reference = characterEntityReferences.convertToReference(character);
        if (reference != null) {
          escaped.append(reference);
        } else {
          escaped.append(character);
        }
      }
    }
    return escaped.toString();
  }

  //---------------------------------------------------------------------
  // Adapted from the Spring Framework org.springframework.web.util.HtmlUtils having the Apache license.
  //---------------------------------------------------------------------
  /**
   * Turn HTML character references into their plain text UNICODE equivalent.
   * <p>Handles complete character set defined in HTML 4.01 recommendation
   * and all reference types (decimal, hex, and entity).
   * <p>Correctly converts the following formats:
   * <blockquote>
   * &amp;#<i>Entity</i>; - <i>(Example: &amp;amp;) case sensitive</i>
   * &amp;#<i>Decimal</i>; - <i>(Example: &amp;#68;)</i><br>
   * &amp;#x<i>Hex</i>; - <i>(Example: &amp;#xE5;) case insensitive</i><br>
   * </blockquote>
   * Gracefully handles malformed character references by copying original
   * characters as is when encountered.<p>
   * <p>Reference:
   * <a HREF="http://www.w3.org/TR/html4/sgml/entities.html">
   * http://www.w3.org/TR/html4/sgml/entities.html
   * </a>
   * @param input the (escaped) input string
   * @return the unescaped string
   */
  public static String htmlUnescape(String input) {
    if (input == null) {
      return null;
    }
    return new HtmlCharacterEntityDecoder(characterEntityReferences, input).decode();
  }
}
