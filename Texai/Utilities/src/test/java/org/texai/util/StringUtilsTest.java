package org.texai.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.apache.log4j.Logger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author reed
 */
public class StringUtilsTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(StringUtilsTest.class);

  public StringUtilsTest() {
  }

  /**
   * Test of floatArrayToString method, of class StringUtils.
   */
  @Test
  public void testFloatArrayToString() {
    LOGGER.info("floatArrayToString");
    float[] floatArray = {1.0f, 2.0f, 3.1f};
    assertEquals("[1.0, 2.0, 3.1]", StringUtils.floatArrayToString(floatArray));
    float[] floatArray2 = {};
    assertEquals("[]", StringUtils.floatArrayToString(floatArray2));
  }

  /**
   * Test of toSortedStrings method, of class StringUtils.
   */
  @Test
  public void testToSortedStrings() {
    LOGGER.info("toSortedStrings");
    final Collection<Object> objects = new HashSet<>();
    objects.add("1");
    objects.add("2");
    objects.add(3);
    objects.add(4);
    objects.add('5');
    objects.add(new ArrayList<>());
    assertEquals("[1, 2, 3, 4, 5, []]", StringUtils.toSortedStrings(objects).toString());

    objects.clear();
    assertEquals("[]", StringUtils.toSortedStrings(objects).toString());
  }

  /**
   * Test of booleanArrayToBitString method, of class StringUtils.
   */
  @Test
  public void testBooleanArrayToBitString() {
    LOGGER.info("booleanArrayToBitString");
    boolean[] booleanArray = {true};
    assertEquals("1", StringUtils.booleanArrayToBitString(booleanArray));
    boolean[] booleanArray2 = {false, true};
    assertEquals("01", StringUtils.booleanArrayToBitString(booleanArray2));
    boolean[] booleanArray3 = {true, false};
    assertEquals("10", StringUtils.booleanArrayToBitString(booleanArray3));
  }

  /**
   * Test of htmlUnescape method, of class StringUtils.
   */
  @Test
  public void testHtmlUnescape() {
    LOGGER.info("htmlUnescape");
    assertEquals("abc", StringUtils.htmlUnescape("abc"));
    assertEquals("a b c", StringUtils.htmlUnescape("a&nbsp;b&nbsp;c"));
    assertEquals("Communicator> hello Stephen", StringUtils.htmlUnescape("Communicator&gt;&nbsp;hello&nbsp;Stephen"));
  }

  /**
   * Test of isJavaClassName method, of class StringUtils.
   */
  @Test
  public void testIsJavaClassName() {
    LOGGER.info("isJavaClassName");
    assertFalse(StringUtils.isJavaClassName(null));
    assertFalse(StringUtils.isJavaClassName(""));
    assertFalse(StringUtils.isJavaClassName(".abc"));
    assertFalse(StringUtils.isJavaClassName("org. texai.Abc"));
    assertFalse(StringUtils.isJavaClassName("org..texai.Abc"));
    assertFalse(StringUtils.isJavaClassName("org.texai.Abc "));
    assertTrue(StringUtils.isJavaClassName("org.texai.Abc"));
  }

  /**
   * Test of isNonEmptyString method, of class StringUtils.
   */
  @Test
  public void testIsNonEmptyString() {
    LOGGER.info("isNonEmptyString");
    assertFalse(StringUtils.isNonEmptyString(null));
    assertFalse(StringUtils.isNonEmptyString(""));
    assertTrue(StringUtils.isNonEmptyString("abc"));
  }

  /**
   * Test of splitOnSpace method, of class StringUtils.
   */
  @Test
  public void testSplitOnSpace() {
    LOGGER.info("splitOnSpace");
    assertEquals("[abc]", StringUtils.splitOnSpace("abc").toString());
    assertEquals("a", StringUtils.splitOnSpace("a b c").get(0));
    assertEquals("b", StringUtils.splitOnSpace("a b c").get(1));
    assertEquals("c", StringUtils.splitOnSpace("a b c").get(2));
    assertEquals("[a, b, c]", StringUtils.splitOnSpace("a b c").toString());
    assertEquals(3, StringUtils.splitOnSpace(" a b c ").size());
    assertEquals("[a, b, c]", StringUtils.splitOnSpace(" a b c ").toString());
    assertEquals("[abc, def]", StringUtils.splitOnSpace("abc def").toString());
  }

  /**
   * Test of splitHTMLTags method, of class StringUtils.
   */
  @Test
  public void testSplitHTMLTags() {
    LOGGER.info("splitHTMLTags");
    assertEquals("[abc]", StringUtils.splitHTMLTags("abc").toString());
    assertEquals("a", StringUtils.splitHTMLTags("a b c").get(0));
    assertEquals("b", StringUtils.splitHTMLTags("a b c").get(1));
    assertEquals("c", StringUtils.splitHTMLTags("a b c").get(2));
    assertEquals("[a, b, c]", StringUtils.splitHTMLTags("a b c").toString());
    assertEquals(3, StringUtils.splitHTMLTags(" a b c ").size());
    assertEquals("[a, b, c]", StringUtils.splitHTMLTags(" a b c ").toString());
    assertEquals("[abc, def]", StringUtils.splitHTMLTags("abc def").toString());
    assertEquals("[abc, <br>]", StringUtils.splitHTMLTags("abc<br>").toString());
    assertEquals("[ab, <br>, c]", StringUtils.splitHTMLTags("ab<br>c").toString());
    assertEquals("[<br>, ab, <br>, c]", StringUtils.splitHTMLTags("<br>ab<br>c").toString());
    assertEquals("[<li>, abc, </li>]", StringUtils.splitHTMLTags("<li>abc</li>").toString());
  }

  /**
   * Test of logStringCharacterDifferences method, of class StringUtils.
   */
  @Test
  public void testLogStringCharacterDifferences() {
    LOGGER.info("logStringCharacterDifferences");
    StringUtils.logStringCharacterDifferences("abc", "abc");
    StringUtils.logStringCharacterDifferences("abc", "adc");
    try {
      StringUtils.logStringCharacterDifferences("abc ", "abc");
      assert false : "failed";
    } catch (AssertionError ex) {
      LOGGER.info("caught expected: " + ex.getMessage());
    }
  }

  /**
   * Test of escapeSingleQuotes method, of class StringUtils.
   */
  @Test
  public void testEscapeSingleQuotes() {
    LOGGER.info("escapeSingleQuotes");
    String string = "abc\"def'ghi";
    String result = StringUtils.escapeSingleQuotes(string);
    assertEquals("abc\"def\\'ghi", result);
  }

  /**
   * Test of getStackTraceAsString method, of class StringUtils.
   */
  @Test
  public void testGetStackTraceAsString() {
    LOGGER.info("getStackTraceAsString");
    try {
      throw new TexaiException("test");
    } catch (final TexaiException ex) {
      String result = StringUtils.getStackTraceAsString(ex);
      assertTrue(result.startsWith("org.texai.util.TexaiException: test"));
    }
  }

  /**
   * Test of removeEnclosingDoubleQuotes method, of class StringUtils.
   */
  @Test
  public void testRemoveEnclosingDoubleQuotes() {
    LOGGER.info("removeEnclosingDoubleQuotes");
    String string = "abc";
    String result = StringUtils.removeEnclosingDoubleQuotes(string);
    assertEquals("abc", result);

    string = "ab\"c";
    result = StringUtils.removeEnclosingDoubleQuotes(string);
    assertEquals("ab\"c", result);

    string = "\"abc";
    result = StringUtils.removeEnclosingDoubleQuotes(string);
    assertEquals("abc", result);

    string = "abc\"";
    result = StringUtils.removeEnclosingDoubleQuotes(string);
    assertEquals("abc", result);

    string = "\"abc\"";
    result = StringUtils.removeEnclosingDoubleQuotes(string);
    assertEquals("abc", result);
  }

  /**
   * Test of convertInputStreamToString method, of class StringUtils.
   */
  @Test
  public void testConvertInputStreamToString() {
    LOGGER.info("convertInputStreamToString");
    final String string = "abcd\n1234";
    InputStream inputStream = null;
    try {
      inputStream = new ByteArrayInputStream(string.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException ex) {
      fail(ex.getMessage());
    }
    assertNotNull(inputStream);
    final String result = StringUtils.convertInputStreamToString(inputStream);
    assertEquals(string, result);
  }

  /**
   * Test of indentSpaces method, of class StringUtils.
   */
  @Test
  public void testindentSpaces() {
    LOGGER.info("indentSpaces");
    final StringBuilder stringBuilder = new StringBuilder();
    assertEquals("", stringBuilder.toString());
    StringUtils.indentSpaces(stringBuilder, 0);
    assertEquals("", stringBuilder.toString());
    StringUtils.indentSpaces(stringBuilder, 10);
    assertEquals("          ", stringBuilder.toString());
    stringBuilder.append('a');
    StringUtils.indentSpaces(stringBuilder, 5);
    assertEquals("          a     ", stringBuilder.toString());
  }

  /**
   * Test of makeBlankString method, of class StringUtils.
   */
  @Test
  public void testMakeBlankString() {
    LOGGER.info("makeBlankString");
    assertEquals("", StringUtils.makeBlankString(0));
    assertEquals(" ", StringUtils.makeBlankString(1));
    assertEquals("  ", StringUtils.makeBlankString(2));
    assertEquals("   ", StringUtils.makeBlankString(3));
    assertEquals("    ", StringUtils.makeBlankString(4));
    assertEquals("     ", StringUtils.makeBlankString(5));
    assertEquals("      ", StringUtils.makeBlankString(6));
  }

  /**
   * Test of getLowerCasePredicateName method, of class StringUtils.
   */
  @Test
  public void testGetLowerCasePredicateName() {
    LOGGER.info("getLowerCasePredicateName");
    assertEquals("a", StringUtils.getLowerCasePredicateName("A"));
    assertEquals("ab", StringUtils.getLowerCasePredicateName("AB"));
    assertEquals("abc", StringUtils.getLowerCasePredicateName("ABC"));
    assertEquals("abcd", StringUtils.getLowerCasePredicateName("ABCD"));
    assertEquals("ab", StringUtils.getLowerCasePredicateName("Ab"));
    assertEquals("abc", StringUtils.getLowerCasePredicateName("Abc"));
    assertEquals("abcd", StringUtils.getLowerCasePredicateName("Abcd"));
    assertEquals("aBc", StringUtils.getLowerCasePredicateName("ABc"));
    assertEquals("aBcd", StringUtils.getLowerCasePredicateName("ABcd"));
    assertEquals("abC", StringUtils.getLowerCasePredicateName("AbC"));
    assertEquals("abCd", StringUtils.getLowerCasePredicateName("AbCd"));
    assertEquals("rdfTestEntity", StringUtils.getLowerCasePredicateName("RDFTestEntity"));
    assertEquals("string", StringUtils.getLowerCasePredicateName("String"));
  }

  /**
   * Test of hasLength method, of class StringUtils.
   */
  @Test
  public void testHasLength() {
    LOGGER.info("hasLength");
    assertTrue(StringUtils.hasLength("abc"));
    assertFalse(StringUtils.hasLength(""));
    assertFalse(StringUtils.hasLength((String) null));
  }

  /**
   * Test of replace method, of class StringUtils.
   */
  @Test
  public void testReplace() {
    LOGGER.info("replace");
    assertEquals("zycdef", StringUtils.replace("abcdef", "ab", "zy"));
  }

  /**
   * Test of toHex method, of class StringUtils.
   */
  @Test
  public void testToHex() {
    System.out.println("toHex");
    byte[] buffer = "".getBytes();
    assertEquals("", StringUtils.toHex(buffer));
    buffer = "0".getBytes();
    assertEquals("30", StringUtils.toHex(buffer));
    buffer = "0123".getBytes();
    assertEquals("30313233", StringUtils.toHex(buffer));
    assertEquals("303132", StringUtils.toHex(buffer, 3));
  }

  /**
   * Test of capitalize method, of class StringUtils.
   */
  @Test
  public void testCapitalize() {
    LOGGER.info("capitalize");
    assertEquals("30313233", StringUtils.capitalize("30313233"));
    assertEquals("Abc", StringUtils.capitalize("abc"));
    assertEquals("Abc", StringUtils.capitalize("Abc"));
  }

  /**
   * Test of uncapitalize method, of class StringUtils.
   */
  @Test
  public void testUncapitalize() {
    LOGGER.info("uncapitalize");
    assertEquals("30313233", StringUtils.uncapitalize("30313233"));
    assertEquals("abc", StringUtils.uncapitalize("Abc"));
    assertEquals("aBC", StringUtils.uncapitalize("ABC"));
  }

  /**
   * Test of packageFromClassName method, of class StringUtils.
   */
  @Test
  public void testPackageFromClassName() {
    LOGGER.info("packageFromClassName");
    assertEquals("java.lang", StringUtils.packageFromClassName(String.class.getName()));
  }

  /**
   * Test of simpleClassName method, of class StringUtils.
   */
  @Test
  public void testSimpleClassName() {
    LOGGER.info("simpleClassName");
    assertEquals("String", StringUtils.simpleClassName(String.class.getName()));
  }

  /**
   * Test of countOccurrencesOf method, of class StringUtils.
   */
  @Test
  public void testCountOccurrencesOf() {
    LOGGER.info("countOccurrencesOf");

    assertEquals(0, StringUtils.countOccurrencesOf("", ""));
    assertEquals(1, StringUtils.countOccurrencesOf("a", "a"));
    assertEquals(2, StringUtils.countOccurrencesOf("aa", "a"));
    assertEquals(0, StringUtils.countOccurrencesOf("a", "aa"));
    assertEquals(3, StringUtils.countOccurrencesOf("\nabc\ndef\n", "\n"));
  }

  /**
   * Test of ensureTwoNewLines method, of class StringUtils.
   */
  @Test
  public void testEnsureTwoNewLines() {
    LOGGER.info("ensureTwoNewLines");

    final StringBuilder stringBuilder = new StringBuilder();
    StringUtils.ensureTwoNewLines(stringBuilder);
    assertEquals("\n\n", stringBuilder.toString());

    stringBuilder.setLength(0);
    stringBuilder.append("abc");
    StringUtils.ensureTwoNewLines(stringBuilder);
    assertEquals("abc\n\n", stringBuilder.toString());

    stringBuilder.setLength(0);
    stringBuilder.append("ab\nc");
    StringUtils.ensureTwoNewLines(stringBuilder);
    assertEquals("ab\nc\n\n", stringBuilder.toString());

    stringBuilder.setLength(0);
    stringBuilder.append("abc\n");
    StringUtils.ensureTwoNewLines(stringBuilder);
    assertEquals("abc\n\n", stringBuilder.toString());

    stringBuilder.setLength(0);
    stringBuilder.append("abc\n\n");
    StringUtils.ensureTwoNewLines(stringBuilder);
    assertEquals("abc\n\n", stringBuilder.toString());
  }

  /**
   * Test of ensureOneNewLine method, of class StringUtils.
   */
  @Test
  public void testEnsureOneNewLine() {
    LOGGER.info("ensureOneNewLine");

    final StringBuilder stringBuilder = new StringBuilder();
    StringUtils.ensureOneNewLine(stringBuilder);
    assertEquals("\n", stringBuilder.toString());

    stringBuilder.setLength(0);
    stringBuilder.append("abc");
    StringUtils.ensureOneNewLine(stringBuilder);
    assertEquals("abc\n", stringBuilder.toString());

    stringBuilder.setLength(0);
    stringBuilder.append("ab\nc");
    StringUtils.ensureOneNewLine(stringBuilder);
    assertEquals("ab\nc\n", stringBuilder.toString());

    stringBuilder.setLength(0);
    stringBuilder.append("abc\n");
    StringUtils.ensureOneNewLine(stringBuilder);
    assertEquals("abc\n", stringBuilder.toString());

    stringBuilder.setLength(0);
    stringBuilder.append("abc\n\n");
    StringUtils.ensureOneNewLine(stringBuilder);
    assertEquals("abc\n\n", stringBuilder.toString());
  }

  /**
   * Test of toSingleString method, of class StringUtils.
   */
  @Test
  public void testToSingleString() {
    LOGGER.info("toSingleString");

    final List<String> strings = new ArrayList<>();
    assertEquals("", StringUtils.toSingleString(strings));

    strings.add("a");
    assertEquals("a", StringUtils.toSingleString(strings));

    strings.clear();
    strings.add("a");
    strings.add("b");
    assertEquals("a b", StringUtils.toSingleString(strings));

    strings.clear();
    strings.add("a");
    strings.add("b");
    strings.add("c");
    assertEquals("a b c", StringUtils.toSingleString(strings));

    strings.clear();
    strings.add("a ");
    strings.add("b ");
    strings.add("c ");
    assertEquals("a  b  c ", StringUtils.toSingleString(strings));
  }

  /**
   * Test of padWithTrailingSpaces method, of class StringUtils.
   */
  @Test
  public void testPadWithTrailingSpaces() {
    LOGGER.info("padWithTrailingSpaces");

    assertEquals("", StringUtils.padWithTrailingSpaces("", 0));
    assertEquals("a", StringUtils.padWithTrailingSpaces("a", 0));
    assertEquals("a", StringUtils.padWithTrailingSpaces("a", 1));
    assertEquals("a ", StringUtils.padWithTrailingSpaces("a", 2));
    assertEquals("abc", StringUtils.padWithTrailingSpaces("abc", 3));
    assertEquals("abc   ", StringUtils.padWithTrailingSpaces("abc", 6));
    assertEquals(" abc    ", StringUtils.padWithTrailingSpaces(" abc", 8));
  }

  /**
   * Test of stripNumericSuffix method, of class StringUtils.
   */
  @Test
  public void testStripNumericSuffix() {
    LOGGER.info("stripNumericSuffix");

    assertEquals("", StringUtils.stripNumericSuffix(""));
    assertEquals("abc", StringUtils.stripNumericSuffix("abc"));
    assertEquals("ab1c", StringUtils.stripNumericSuffix("ab1c"));
    assertEquals("", StringUtils.stripNumericSuffix("1"));
    assertEquals("", StringUtils.stripNumericSuffix("12"));
    assertEquals("", StringUtils.stripNumericSuffix("123"));
    assertEquals("abc", StringUtils.stripNumericSuffix("abc1"));
    assertEquals("abc", StringUtils.stripNumericSuffix("abc12"));
    assertEquals("abc", StringUtils.stripNumericSuffix("abc123"));
  }

  /**
   * Test of htmlEscape method, of class StringUtils.
   */
  @Test
  public void testHtmlEscape() {
    LOGGER.info("htmlEscape");

    assertNull("", StringUtils.htmlEscape(null));
    assertEquals("", StringUtils.htmlEscape(""));
    assertEquals("abc", StringUtils.htmlEscape("abc"));
    assertEquals("<br>", StringUtils.htmlEscape("\n"));
    assertEquals("&nbsp;", StringUtils.htmlEscape(" "));
    assertEquals("&lt;tag&gt;", StringUtils.htmlEscape("<tag>"));
    assertEquals("&amp;", StringUtils.htmlEscape("&"));
  }

  /**
   * Test of stripPunctuationAndDigits method, of class StringUtils.
   */
  @Test
  public void testStripPunctuationAndDigits() {
    LOGGER.info("stripPunctuationAndDigits");

    assertEquals(" ", StringUtils.stripPunctuationAndDigits(" "));
    assertEquals("abc def", StringUtils.stripPunctuationAndDigits("abc def"));
    assertEquals("abc def", StringUtils.stripPunctuationAndDigits("abc-def"));
    assertEquals("abc", StringUtils.stripPunctuationAndDigits("abc"));
    assertEquals("", StringUtils.stripPunctuationAndDigits("123"));
    assertEquals("abc def ghi", StringUtils.stripPunctuationAndDigits("abc, def, ghi."));
    assertEquals("abc def ghi", StringUtils.stripPunctuationAndDigits("abc, (def), [ghi]."));
  }

  /**
   * Test of isOrderConsistent method, of class StringUtils.
   */
  @Test
  public void testIsOrderConsistent() {
    LOGGER.info("isOrderConsistent");

    final List<String> strings1 = new ArrayList<>();
    final List<String> strings2 = new ArrayList<>();
    assertTrue(StringUtils.isOrderConsistent(strings1, strings2));

    strings1.add("a");
    assertFalse(StringUtils.isOrderConsistent(strings1, strings2));

    strings2.add("a");
    assertTrue(StringUtils.isOrderConsistent(strings1, strings2));

    strings1.add("b");
    // [a, b] vs [a]
    assertFalse(StringUtils.isOrderConsistent(strings1, strings2));

    strings2.add("c");
    // [a, b] vs [a, c]
    assertFalse(StringUtils.isOrderConsistent(strings1, strings2));

    strings1.add("c");
    // [a, b, c] vs [a, c]
    assertTrue(StringUtils.isOrderConsistent(strings1, strings2));

    strings1.add("d");
    strings2.add("d");
    // [a, b, c, d] vs [a, c, d]
    assertTrue(StringUtils.isOrderConsistent(strings1, strings2));

    strings1.add("e");
    strings2.add("f");
    // [a, b, c, d, e] vs [a, c, d, f]
    assertFalse(StringUtils.isOrderConsistent(strings1, strings2));

    strings1.clear();
    strings1.add("The");
    strings1.add("first");
    strings1.add("cat");
    strings1.add("on");
    strings1.add("the");
    strings1.add("book");
    strings2.clear();
    strings2.add("the");
    strings2.add("first");
    strings2.add("cat");
    strings2.add("on");
    strings2.add("the");
    strings2.add("book");
    assertTrue(StringUtils.isOrderConsistent(strings1, strings2));

    strings2.clear();
    strings2.add("the");
    strings2.add("first");
    strings2.add("book");
    strings2.add("on");
    strings2.add("the");
    strings2.add("cat");
    assertFalse(StringUtils.isOrderConsistent(strings1, strings2));
  }



}
