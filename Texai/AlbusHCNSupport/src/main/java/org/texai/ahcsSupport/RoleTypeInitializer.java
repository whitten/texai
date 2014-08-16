/*
 * RoleTypeInitializer.java
 *
 * Created on May 6, 2010, 6:00:44 PM
 *
 * Description: Initializes role types from an XML file.
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

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.jcip.annotations.NotThreadSafe;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.texai.ahcsSupport.domainEntity.RoleType;
import org.texai.ahcsSupport.domainEntity.SkillClass;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.util.TexaiException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Initializes role types from an XML file.
 *
 * @author reed
 */
@NotThreadSafe
public class RoleTypeInitializer {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(RoleTypeInitializer.class);
  /** the indicator whether debug logging is enabled */
  private static final boolean IS_DEBUG_LOGGING_ENABLED = LOGGER.isDebugEnabled();
  /** the RDF entity manager */
  private RDFEntityManager rdfEntityManager;
  /** the id assigned by the persistence framework for the role type */
  private URI id;
  /** the role type ids */
  private final List<URI> ids = new ArrayList<>();
  /** the role type name */
  private String typeName;
  /** the names of inherited role types */
  private Set<String> inheritedRoleTypeNames = new HashSet<>();
  /** the skill class name */
  private String skillClassName;
  /** the skill class uses */
  private Set<SkillClass> skillClasses = new HashSet<>();
  /** the role's description in English */
  private String description;
  /** the Albus hierarchical control system granularity level */
  private URI albusHCSGranularityLevel;
  /** the inherited role type dictionary, id --> set of names of inherited role types */
  private final Map<URI, Set<String>> inheritedRoleTypeDictionary = new HashMap<>();
  /** the node access */
  private NodeAccess nodeAccess;
  /** the indicator that parsing is within the scope of a <skill-class> tag */
  private boolean isWithinSkillClassScope = false;
  /** the id assigned by the persistence framework for the skill use */
  private URI skillUseId;

  /** Constructs a new RoleTypeInitializer instance. */
  public RoleTypeInitializer() {
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

  /** Reads the file and persists the role types after all have been parsed.
   *
   * @param roleTypesPath role types file path
   */
  public void process(final String roleTypesPath) {
    //Preconditions
    assert roleTypesPath != null : "roleTypesPath must not be null";
    assert !roleTypesPath.isEmpty() : "roleTypesPath must not be an empty string";

    resetState();

    final BufferedInputStream bufferedInputStream;
    try {
      final File roleTypesFile = new File(roleTypesPath);
      LOGGER.info("parsing the role types file: " + roleTypesFile.toString());
      bufferedInputStream = new BufferedInputStream(new FileInputStream(roleTypesFile));
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
    resolveInheritedRoleTypeNames();
    displayLoadedRoleTypes();
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
      switch (qName) {
        case "semantics":
        case "statements":
        case "skill-class":
          isWithinSkillClassScope = true;
          break;
      }
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
      assert rdfEntityManager != null : "rdfEntityManager must not be null";

      if (IS_DEBUG_LOGGING_ENABLED) {
        LOGGER.debug("endElement qName: " + qName);
        LOGGER.debug("stringBuilder: " + stringBuilder.toString());
      }

      if (qName.equals("id") && stringBuilder.length() > 0) {
        if (isWithinSkillClassScope) {
          try {
            skillUseId = new URIImpl(stringBuilder.toString());
          } catch (IllegalArgumentException ex) {
            LOGGER.fatal("invalid id URI: '" + stringBuilder.toString() + "'");
            throw new TexaiException(ex);
          }
        } else {
          try {
            id = new URIImpl(stringBuilder.toString());
          } catch (IllegalArgumentException ex) {
            LOGGER.fatal("invalid id URI: '" + stringBuilder.toString() + "'");
            throw new TexaiException(ex);
          }
        }
      } else if (qName.equals("name")) {
        typeName = stringBuilder.toString();
      } else if (qName.equals("inherited-role-type-name") && stringBuilder.length() > 0) {
        inheritedRoleTypeNames.add(stringBuilder.toString());
      } else if (qName.equals("skill-class-name") && stringBuilder.length() > 0) {
        skillClassName = stringBuilder.toString();
      } else if (qName.equals("skill-class")) {
        final SkillClass skillClass = new SkillClass(skillClassName);
        if (skillUseId != null) {
          rdfEntityManager.setIdFor(skillClass, skillUseId);
        }
        skillClasses.add(skillClass);
        skillClassName = null;
        isWithinSkillClassScope = false;
        LOGGER.info("persisting " + skillClass);
        rdfEntityManager.persist(skillClass);
      } else if (qName.equals("description")) {
        description = stringBuilder.toString();
      } else if (qName.equals("granularity-level")) {
        albusHCSGranularityLevel = new URIImpl(Constants.TEXAI_NAMESPACE + stringBuilder.toString());
      } else if (qName.equals("role-type")) {

        RoleType roleType = nodeAccess.findRoleType(typeName);

        if (roleType == null) {
          roleType = new RoleType();
        } else {
          roleType.clearSkillUses();
        }
        if (albusHCSGranularityLevel != null) {
          roleType.setAlbusHCSGranularityLevel(albusHCSGranularityLevel);
        }
        if (description != null && !description.isEmpty()) {
          roleType.setDescription(description);
        }
        if (typeName != null && !typeName.isEmpty()) {
          roleType.setTypeName(typeName);
        }
        for (final SkillClass skillUse : skillClasses) {
          roleType.addSkillUse(skillUse);
        }
        if (IS_DEBUG_LOGGING_ENABLED) {
          LOGGER.debug("\n" + roleType.toXML());
        }
        if (id != null) {
          rdfEntityManager.setIdFor(roleType, id);
        }
        rdfEntityManager.persist(roleType);
        LOGGER.info("persisting: " + roleType.toString() + "  id: " + roleType.getId());
        assert isLoadedCorrectly(roleType);
        ids.add(roleType.getId());
        if (!inheritedRoleTypeNames.isEmpty()) {
          inheritedRoleTypeDictionary.put(roleType.getId(), Collections.unmodifiableSet(inheritedRoleTypeNames));
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
    inheritedRoleTypeNames = new HashSet<>();
    skillClassName = null;
    skillUseId = null;
    skillClasses = new HashSet<>();
    isWithinSkillClassScope = false;
    description = null;
    albusHCSGranularityLevel = null;
  }

  /** Verifies that the given role type has been persisted correctly.
   *
   * @param roleType the given role type
   * @return true if no assertion errors occur.
   */
  private boolean isLoadedCorrectly(final RoleType roleType) {
    //Preconditions
    assert nodeAccess != null : "nodeAccess must not be null";

    final List<RoleType> existingRoleTypes = nodeAccess.findRoleTypes(roleType.getTypeName());
    assert existingRoleTypes.size() == 1;
    if (IS_DEBUG_LOGGING_ENABLED) {
      LOGGER.debug("persisted\n" + roleType.toXML());
      LOGGER.debug("loaded\n" + existingRoleTypes.get(0).toXML());
    }
    LOGGER.info("persisted\n" + roleType.toXML());
    LOGGER.info("loaded\n" + existingRoleTypes.get(0).toXML());
    assert existingRoleTypes.get(0).toXML().equals(roleType.toXML());
    return true;
  }

  /** Resolves inherited role type names. */
  private void resolveInheritedRoleTypeNames() {
    //Preconditions
    assert nodeAccess != null : "nodeAccess must not be null";
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    // resolve the inherited role type names, which might have been forward references
    for (final Entry<URI, Set<String>> entry : inheritedRoleTypeDictionary.entrySet()) {
      final URI id1 = entry.getKey();
      final RoleType loadedRoleType = rdfEntityManager.find(RoleType.class, id1);
      assert loadedRoleType != null;
      final Set<String> inheritedRoleTypeNames1 = entry.getValue();
      assert !inheritedRoleTypeNames1.isEmpty();
      for (final String inheritedRoleTypeName : inheritedRoleTypeNames1) {
        final RoleType inheritedRoleType = nodeAccess.findRoleType(inheritedRoleTypeName);
        if (inheritedRoleType == null) {
          throw new TexaiException("inherited node type not found: " + inheritedRoleTypeName);
        }
        loadedRoleType.addInheritedRoleType(inheritedRoleType);
      }
      rdfEntityManager.persist(loadedRoleType);
    }
  }

  /** Displays the loaded role types. */
  private void displayLoadedRoleTypes() {
    for (final URI id1 : ids) {
      final RoleType loadedRoleType = rdfEntityManager.find(RoleType.class, id1);
      LOGGER.info("\n" + loadedRoleType.toXML(2));
    }
  }

  /** Finalizes this application. */
  public void finalization() {
    LOGGER.info("RoleTypeInitializer completed");
  }

  /** Executes this application.
   *
   * @param args the command line arguments (not used)
   */
  public static void main(final String[] args) {
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.clearNamedRepository("NodeRoleTypes");
    final RoleTypeInitializer roleTypeInitializer =
            new RoleTypeInitializer();
    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    roleTypeInitializer.initialize(rdfEntityManager); // clear repository first
    roleTypeInitializer.process("data/role-types.xml");
    roleTypeInitializer.finalization();
    rdfEntityManager.close();
    DistributedRepositoryManager.shutDown();
    CacheManager.getInstance().shutdown();
  }
}
