/*
 * XMLUtils.java
 *
 * Created on Apr 11, 2012, 8:13:04 PM
 *
 * Description: Provides XML utilities.
 *
 * Copyright (C) Apr 11, 2012, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.jcip.annotations.NotThreadSafe;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** Provides XML utilities.
 *
 * @author reed
 */
@NotThreadSafe
public class XMLUtils {

  /** Prevents the construction of an XMLUtils instance. */
  private XMLUtils() {
  }

  /** Loads an XML document from the given XML string.
   *
   * @param xmlString the given XML string
   * @return the XML document
   */
  public static Document loadXMLFromString(final String xmlString) {
    //Preconditions
    assert StringUtils.isNonEmptyString(xmlString) : "xmlString must be a non-empty string";

    try {
      final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      final InputSource inputSource = new InputSource(new StringReader(xmlString));
      return documentBuilder.parse(inputSource);
    } catch (SAXException | IOException | ParserConfigurationException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Formats the given XML document.
   *
   * @param xmlString the given XML document
   * @return the formatted XML string
   */
  public static String prettyPrintWithDOM3LS(final String xmlString) {
    //Preconditions
    assert StringUtils.isNonEmptyString(xmlString) : "xmlString must be a non-empty string";

    return prettyPrintWithDOM3LS(loadXMLFromString(xmlString));
  }

  /** Formats the given XML document.
   *
   * @param document the given XML document
   * @return the formatted XML string
   */
  public static String prettyPrintWithDOM3LS(final Document document) {
    //Preconditions
    assert document != null : "document must not be null";

    final DOMImplementation domImplementation = document.getImplementation();
    if (domImplementation.hasFeature("LS", "3.0") && domImplementation.hasFeature("Core", "2.0")) {
      final DOMImplementationLS domImplementationLS = (DOMImplementationLS) domImplementation.getFeature("LS", "3.0");
      final LSSerializer lsSerializer = domImplementationLS.createLSSerializer();
      final DOMConfiguration domConfiguration = lsSerializer.getDomConfig();
      if (domConfiguration.canSetParameter("format-pretty-print", Boolean.TRUE)) {
        lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
        LSOutput lsOutput = domImplementationLS.createLSOutput();
        lsOutput.setEncoding("UTF-8");
        final StringWriter stringWriter = new StringWriter();
        lsOutput.setCharacterStream(stringWriter);
        lsSerializer.write(document, lsOutput);
        return stringWriter.toString();
      } else {
        throw new RuntimeException("DOMConfiguration 'format-pretty-print' parameter isn't settable.");
      }
    } else {
      throw new RuntimeException("DOM 3.0 LS and/or DOM 2.0 Core not supported.");
    }
  }
}
