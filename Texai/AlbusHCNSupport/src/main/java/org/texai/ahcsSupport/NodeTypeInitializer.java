/*
 * NodeTypeInitializer.java
 *
 * Created on May 6, 2010, 6:00:57 PM
 *
 * Description: Initializes node types from an XML file.
 *
 * Copyright (C) May 6, 2010, Stephen L. Reed.
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
package org.texai.ahcsSupport;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.jcip.annotations.NotThreadSafe;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.texai.ahcsSupport.domainEntity.NodeType;
import org.texai.ahcsSupport.domainEntity.RoleType;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Initializes node types from an XML file.
 *
 * @author reed
 */
@NotThreadSafe
public class NodeTypeInitializer {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(NodeTypeInitializer.class);
  /** the indicator whether debug logging is enabled */
  private static final boolean IS_DEBUG_LOGGING_ENABLED = LOGGER.isDebugEnabled();
  /** the RDF entity manager */
  private RDFEntityManager rdfEntityManager;
  /** the id assigned by the persistence framework */
  private URI id;
  /** the node type ids */
  private final List<URI> ids = new ArrayList<>();
  /** the node type name */
  private String typeName;
  /** the names of inherited node types */
  private Set<String> inheritedNodeTypeNames = new HashSet<>();
  /** the names of role types */
  private Set<String> roleTypeNames = new HashSet<>();
  /** the node's mission description in English */
  private String missionDescription;
  /** the inherited node type dictionary, id --> set of names of inherited node types */
  private final Map<URI, Set<String>> inheritedNodeTypeDictionary = new HashMap<>();
  /** the node access */
  private NodeAccess nodeAccess;

  /** Constructs a new NodeTypeInitializer instance. */
  public NodeTypeInitializer() {
  }

  /** Initializes the application.
   *
   * @param rdfEntityManager the RDF entity manager
   * rule unit test specifications
   */
  public void initialize(final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    this.rdfEntityManager = rdfEntityManager;
    nodeAccess = new NodeAccess(rdfEntityManager);
  }

  /** Reads the file and persists the node types after all have been parsed.
   *
   * @param nodeTypesPath node types file path
   */
  public void process(final String nodeTypesPath) {
    //Preconditions
    assert nodeTypesPath != null : "nodeTypesPath must not be null";
    assert !nodeTypesPath.isEmpty() : "nodeTypesPath must not be an empty string";

    resetState();

    final BufferedInputStream bufferedInputStream;
    try {
      final File nodeTypesFile = new File(nodeTypesPath);
      LOGGER.info("parsing the node types file: " + nodeTypesFile.toString());
      bufferedInputStream = new BufferedInputStream(new FileInputStream(nodeTypesFile));
    } catch (final FileNotFoundException ex) {
      throw new TexaiException(ex);
    }
    try {
      final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      final SAXParser saxParser = saxParserFactory.newSAXParser();
      final SAXHandler myHandler = new SAXHandler();
      saxParser.parse(bufferedInputStream, myHandler);

    } catch (final ParserConfigurationException | SAXException | IOException ex) {
      LOGGER.fatal(StringUtils.getStackTraceAsString(ex));
      throw new TexaiException(ex);
    }
    resolveInheritedNodeTypeNames();
    displayLoadedNodeTypes();
    try {
      final RepositoryConnection repositoryConnection =
              DistributedRepositoryManager.getInstance().getRepositoryConnectionForRepositoryName("NodeRoleTypes");
      LOGGER.info("repository size: " + repositoryConnection.size());
      repositoryConnection.close();
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Provides a SAX parsing handler. */
  final class SAXHandler extends DefaultHandler {

    /** the string builder */
    private final StringBuilder stringBuilder = new StringBuilder();

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

      if (IS_DEBUG_LOGGING_ENABLED) {
        LOGGER.debug("startElement qName: " + qName);
      }
      stringBuilder.setLength(0);
    }

    /** Receive notification of character data inside an element.
     *
     * @param characters the characters
     * @param start the start position in the character array
     * @param length the length of the character string
     */
    @Override
    public void characters(final char[] characters,
            final int start,
            final int length) {

      //LOGGER.debug("characters, start: " + start + ", length: " + length);
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
      assert nodeAccess != null : "nodeAccess must not be null";

      if (IS_DEBUG_LOGGING_ENABLED) {
        LOGGER.debug("endElement qName: " + qName);
        LOGGER.debug("stringBuilder: " + stringBuilder.toString());
      }

      if (qName.equals("id") && stringBuilder.length() > 0) {
        id = new URIImpl(stringBuilder.toString());
      } else if (qName.equals("name")) {
        typeName = stringBuilder.toString();
      } else if (qName.equals("inherited-node-type-name") && stringBuilder.length() > 0) {
        inheritedNodeTypeNames.add(stringBuilder.toString());
      } else if (qName.equals("role-type-name") && stringBuilder.length() > 0) {
        roleTypeNames.add(stringBuilder.toString());
      } else if (qName.equals("mission")) {
        missionDescription = stringBuilder.toString();
      } else if (qName.equals("node-type")) {

        NodeType nodeType = null;
        final List<NodeType> existingNodeTypes = nodeAccess.findNodeTypes(typeName);

        if (existingNodeTypes.size() == 1) {
          nodeType = existingNodeTypes.get(0);
        }
        if (existingNodeTypes.size() > 1) {
          // corrupt store, remove the multiple existing node type objects having the current node type name
          for (final NodeType existingNodeType : existingNodeTypes) {
            LOGGER.warn("removing unexpected multiple existing node types for " + typeName);
            rdfEntityManager.remove(existingNodeType);
          }
        }
        if (nodeType == null) {
          nodeType = new NodeType();
        } else {
          nodeType.clearInheritedNodeTypes();
          nodeType.clearRoleTypes();
        }
        if (missionDescription != null && !missionDescription.isEmpty()) {
          nodeType.setMissionDescription(missionDescription);
        }
        if (typeName != null && !typeName.isEmpty()) {
          nodeType.setTypeName(typeName);
        }
        for (final String roleTypeName : roleTypeNames) {
          final RoleType roleType = nodeAccess.findRoleType(roleTypeName);
          if (roleType == null) {
            throw new TexaiException("role type not found: " + roleTypeName);
          } else {
            nodeType.addRoleType(roleType);
          }
        }
        if (IS_DEBUG_LOGGING_ENABLED) {
          LOGGER.debug("\n" + nodeType.toXML());
        }
        if (id != null) {
          rdfEntityManager.setIdFor(nodeType, id);
        }
        rdfEntityManager.persist(nodeType);
        LOGGER.info("persisted: " + nodeType.toString() + "  id: " + nodeType.getId());
        assert isLoadedCorrectly(nodeType);
        ids.add(nodeType.getId());
        if (!inheritedNodeTypeNames.isEmpty()) {
          inheritedNodeTypeDictionary.put(nodeType.getId(), Collections.unmodifiableSet(inheritedNodeTypeNames));
        }
        resetState();
      }
      stringBuilder.setLength(0);
    }
  }

  /** Resets the parsing state for the next role type definition. */
  private void resetState() {
    id = null;
    typeName = null;
    inheritedNodeTypeNames = new HashSet<>();
    roleTypeNames = new HashSet<>();
    missionDescription = null;
  }

  /** Verifies that the given node type has been persisted correctly.
   *
   * @param nodeType the given node type
   * @return true if no assertion errors occur.
   */
  private boolean isLoadedCorrectly(final NodeType nodeType) {
    //Preconditions
    assert nodeAccess != null : "nodeAccess must not be null";

    final List<NodeType> existingNodeTypes = nodeAccess.findNodeTypes(nodeType.getTypeName());
    assert existingNodeTypes.size() == 1;
    if (IS_DEBUG_LOGGING_ENABLED) {
      LOGGER.debug("persisted\n" + nodeType.toXML());
      LOGGER.debug("loaded\n" + existingNodeTypes.get(0).toXML());
    }
    return true;
  }

  /** Resolves inherited node type names. */
  private void resolveInheritedNodeTypeNames() {
    //Preconditions
    assert nodeAccess != null : "nodeAccess must not be null";

    // resolve the inherited node type names, which might have been forward references
    for (final Entry<URI, Set<String>> entry : inheritedNodeTypeDictionary.entrySet()) {
      final URI id1 = entry.getKey();
      final NodeType loadedNodeType = rdfEntityManager.find(NodeType.class, id1);
      assert loadedNodeType != null;
      final Set<String> inheritedNodeTypeNames1 = entry.getValue();
      assert !inheritedNodeTypeNames1.isEmpty();
      for (final String inheritedNodeTypeName : inheritedNodeTypeNames1) {
        final NodeType inheritedNodeType = nodeAccess.findNodeType(inheritedNodeTypeName);
        if (inheritedNodeType == null) {
          throw new TexaiException("inherited node type not found: " + inheritedNodeTypeName);
        }
        loadedNodeType.addInheritedNodeType(inheritedNodeType);
      }
      rdfEntityManager.persist(loadedNodeType);
    }
  }

  /** Displays the loaded role types. */
  private void displayLoadedNodeTypes() {
    for (final URI id1 : ids) {
      final NodeType loadedNodeType = rdfEntityManager.find(NodeType.class, id1);
      LOGGER.info("\n" + loadedNodeType.toXML(2));
    }
  }

  /** Finalizes this application. */
  public void finalization() {
    LOGGER.info("NodeTypeInitializer completed");
  }

  /** Executes this application.
   *
   * @param args the command line arguments (not used)
   */
  public static void main(final String[] args) {
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.clearNamedRepository("NodeRoleTypes");
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);

    // initialize the role types
    final RoleTypeInitializer roleTypeInitializer =
            new RoleTypeInitializer();
    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    roleTypeInitializer.initialize(rdfEntityManager);
    roleTypeInitializer.process("data/role-types.xml");
    roleTypeInitializer.finalization();

    // initialize the node types
    final NodeTypeInitializer nodeTypeInitializer =
            new NodeTypeInitializer();
    nodeTypeInitializer.initialize(rdfEntityManager);
    nodeTypeInitializer.process("data/node-types.xml");
    nodeTypeInitializer.finalization();
    rdfEntityManager.close();
    DistributedRepositoryManager.shutDown();
    CacheManager.getInstance().shutdown();
  }
}
