/*
 * QueryContainerInitializer.java
 *
 * Created on Feb 9, 2009, 9:12:07 AM
 *
 * Description: Provides the ability to read named SPARQL queries from a file.
 *
 * Copyright (C) Feb 9, 2009 Stephen L. Reed.
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
package org.texai.inference;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.jcip.annotations.NotThreadSafe;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.texai.inference.rete.ReteEngine;
import org.texai.inference.sparqlParser.SPARQLParser;
import org.texai.inference.sparql.domainEntity.QueryContainer;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.util.TexaiException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Provides the ability to read named SPARQL queries from a file.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public class QueryContainerInitializer {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(QueryContainerInitializer.class);
  /** the RDF entity manager */
  private RDFEntityManager kbEntityManager;
  /** the inference access object */
  private InferenceAccess inferenceAccess;
  /** the indicator whether existing queries having the same name are to be replaced */
  private boolean areQueriesReplaced = false;
  /** the rete engine */
  private ReteEngine reteEngine;
  /** the indicator whether to initialize the query container cache rather than persist each query container into the KB */
  private boolean isCached = false;

  /** Constructs a new QueryContainerInitializer instance. */
  public QueryContainerInitializer() {
  }

  /** Initializes the application.
   *
   * @param rdfEntityManager the RDF entity manager
   * @param areQueriesReplaced the indicator whether existing queries having the same name are to be replaced
   * @param overrideContext the user's belief context, or null to persist to each object's default context
   */
  public void initialize(
          final RDFEntityManager rdfEntityManager,
          final boolean areQueriesReplaced) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    this.areQueriesReplaced = areQueriesReplaced;
    this.kbEntityManager = rdfEntityManager;
    inferenceAccess = new InferenceAccess(rdfEntityManager);
  }

  /** Reads the file and persists the queries after all have been parsed.
   *
   * @param queriesPath the queries file path
   */
  public void process(final String queriesPath) {
    //Preconditions
    assert queriesPath != null : "queriesPath must not be null";
    assert !queriesPath.isEmpty() : "queriesPath must not be an empty string";

    final BufferedInputStream bufferedInputStream;
    try {
      final File queriesFile = new File(queriesPath);
      LOGGER.info("parsing the queries file " + queriesFile.toString());
      bufferedInputStream = new BufferedInputStream(new FileInputStream(queriesFile));
    } catch (final FileNotFoundException ex) {
      throw new TexaiException(ex);
    }
    try {
      final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      final SAXParser saxParser = saxParserFactory.newSAXParser();
      final SAXHandler myHandler = new SAXHandler();
      saxParser.parse(bufferedInputStream, myHandler);

    } catch (final ParserConfigurationException | SAXException | IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Finalizes this application. */
  public void finalization() {
    LOGGER.info("QueryContainerInitializer completed");
  }

  /** Gets the rete engine.
   *
   * @return the rete engine
   */
  public ReteEngine getReteEngine() {
    return reteEngine;
  }

  /** Sets the rete engine.
   * @param reteEngine the rete engine
   */
  public void setReteEngine(final ReteEngine reteEngine) {
    this.reteEngine = reteEngine;
  }

  /** Provides a SAX parsing handler. */
  class SAXHandler extends DefaultHandler {

    /** the string builder */
    private final StringBuilder stringBuilder = new StringBuilder();
    /** the query container name */
    private String name;
    /** the SPARQL query string */
    private String queryString;
    /** the SPARQL parser */
    private final SPARQLParser sparqlParser = new SPARQLParser();

    /** Constructs a new SAXHandler instance. */
    public SAXHandler() {
    }

    /** Receives notification of the start of an element.
     *
     * @param uri the element tag
     * @param localName the local name
     * @param qName the qualified name
     * @param attributes the attributes
     */
    @Override
    public void startElement(
            final String uri,
            final String localName,
            final String qName,
            final Attributes attributes) {
      //Preconditions
      assert qName != null : "qName must not be null";
      assert !qName.isEmpty() : "qName must not be empty";

      LOGGER.debug("startElement qName: " + qName);
      stringBuilder.setLength(0);
    }

    /** Receive notification of character data inside an element.
     *
     * @param characters the characters
     * @param start the start position in the character array
     * @param length the length of the character string
     */
    @Override
    public void characters(
            final char[] characters,
            final int start,
            final int length) {

      LOGGER.debug("characters, start: " + start + ", length: " + length);
      final int end = start + length;
      for (int i = start; i < end; i++) {
        stringBuilder.append(characters[i]);
      }
    }

    /** Receives notification of the end of an element.
     *
     * @param uri the element tag
     * @param localName the local name
     * @param qName the qualified name
     */
    @Override
    public void endElement(
            final String uri,
            final String localName,
            final String qName) {
      //Preconditions
      assert qName != null : "qName must not be null";
      assert !qName.isEmpty() : "qName must not be empty";

      LOGGER.debug("endElement qName: " + qName);
      LOGGER.debug("stringBuilder:\n" + stringBuilder.toString());
      switch (qName) {
        case "name":
          name = stringBuilder.toString().trim();
          break;

        case "SPARQL":
          queryString = stringBuilder.toString();
          final QueryContainer queryContainer = sparqlParser.parseQuery(queryString, name);
          if (isCached) {
            inferenceAccess.cacheQueryContainer(queryContainer);
          } else {
            inferenceAccess.persistQueryContainer(queryContainer, areQueriesReplaced);
          }
          if (reteEngine != null && !reteEngine.containsQueryContainer(queryContainer.getName())) {
            reteEngine.addQueryContainer(queryContainer);
          }
          break;
      }
      stringBuilder.setLength(0);

    }
  }

  /** Gets the indicator whether to initialize the query container cache rather than persist each query container into the KB.
   *
   * @return the isCached the indicator whether to initialize the query container cache rather than persist query container rule into the KB
   */
  public boolean isCached() {
    return isCached;
  }

  /** Sets the indicator whether to initialize the query container cache rather than persist each query container into the KB.
   *
   * @param isCached the indicator whether to initialize the query container cache rather than persist each query container into the KB
   */
  public void setIsCached(final boolean isCached) {
    this.isCached = isCached;
  }

  /** Executes this application.
   *
   * @param args the command line arguments (not used)
   */
  public static void main(final String[] args) {
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addRepositoryPath(
            "InferenceRules",
            System.getenv("REPOSITORIES_TMPFS") + "/InferenceRules");
    DistributedRepositoryManager.clearNamedRepository("InferenceRules");
    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    final QueryContainerInitializer queryContainerInitializer = new QueryContainerInitializer();
    queryContainerInitializer.initialize(
            rdfEntityManager,
            false);  // areQueriesReplaced
//    queryContainerInitializer.process("data/test-query-1.xml");
    queryContainerInitializer.process("../Main/data/bootstrap-queries.xml");
    queryContainerInitializer.finalization();
    rdfEntityManager.close();
    DistributedRepositoryManager.shutDown();
    CacheManager.getInstance().shutdown();
  }
}
