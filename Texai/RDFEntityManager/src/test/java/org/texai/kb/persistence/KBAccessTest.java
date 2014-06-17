/*
 * KBAccessTest.java
 *
 * Created on Jun 30, 2008, 1:10:04 PM
 *
 * Description: .
 *
 * Copyright (C) Nov 11, 2010 reed.
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
package org.texai.kb.persistence;

import java.util.HashSet;
import java.util.Set;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.object.AbstractKBObject;
import org.texai.kb.object.ClassKBObject;
import org.texai.kb.restriction.domainEntity.AbstractRestriction;
import org.texai.kb.restriction.domainEntity.AllValuesFromRestriction;
import org.texai.kb.restriction.domainEntity.CardinalityRestriction;
import org.texai.kb.restriction.domainEntity.HasValueRestriction;
import org.texai.kb.restriction.domainEntity.MaxCardinalityRestriction;
import org.texai.kb.restriction.domainEntity.MinCardinalityRestriction;
import org.texai.kb.restriction.domainEntity.SomeValuesFromRestriction;

/**
 *
 * @author reed
 */
public class KBAccessTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(KBAccessTest.class);
  /** the RDF entity manager */
  private static RDFEntityManager rdfEntityManager = new RDFEntityManager();
  /** the OpenCyc repository name */
  private static final String OPEN_CYC = "OpenCyc";

  public KBAccessTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    Logger.getLogger(KBAccess.class).setLevel(Level.DEBUG);
    CacheInitializer.initializeCaches();
    JournalWriter.deleteJournalFiles();
    DistributedRepositoryManager.addRepositoryPath(
            "ConceptuallyRelatedTerms",
            System.getenv("REPOSITORIES_TMPFS") + "/Test");
    DistributedRepositoryManager.addRepositoryPath(
            "OpenCyc",
            "data/repositories/OpenCyc");
    DistributedRepositoryManager.clearNamedRepository("Test");
    final KBAccess kbAccess = new KBAccess(rdfEntityManager);
    URI predicate = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final URI actionTerm = new URIImpl(Constants.CYC_NAMESPACE + "Action");

    kbAccess.addAllValuesFromRestriction(
            OPEN_CYC, // repository name
            actionTerm, // subject
            predicate, // onProperty
            new URIImpl(Constants.CYC_NAMESPACE + "Agent-Generic")); // allValuesClass

    kbAccess.addSomeValuesFromRestriction(
            OPEN_CYC, // repository name
            new URIImpl(Constants.CYC_NAMESPACE + "Snowboarding"), // subject
            predicate, // onProperty
            new URIImpl(Constants.CYC_NAMESPACE + "MaleHuman")); // someValuesClass

    kbAccess.addCardinalityRestriction(
            OPEN_CYC, // repository name
            new URIImpl(Constants.CYC_NAMESPACE + "DeckOfCards"), // subject
            new URIImpl(Constants.CYC_NAMESPACE + "parts"), // onProperty
            52); // cardinality

    kbAccess.addMinCardinalityRestriction(
            OPEN_CYC, // repository name
            new URIImpl(Constants.CYC_NAMESPACE + "Person"), // subject
            new URIImpl(Constants.CYC_NAMESPACE + "fosterFather"), // onProperty
            0); // cardinality

    kbAccess.addMaxCardinalityRestriction(
            OPEN_CYC, // repository name
            new URIImpl(Constants.CYC_NAMESPACE + "Person"), // subject
            new URIImpl(Constants.CYC_NAMESPACE + "biologicalFather"), // onProperty
            1); // cardinality
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    JournalWriter.close();
    rdfEntityManager.close();
    DistributedRepositoryManager.shutDown();
    CacheManager.getInstance().shutdown();
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of findKBObject method, of class KBAccess.
   */
  @Test
  public void testFindKBObject() {
    LOGGER.info("findKBObject");
    final KBAccess instance = new KBAccess(rdfEntityManager);
    AbstractKBObject result = instance.findKBObject(OPEN_CYC, new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat"));
    assertEquals("cyc:DomesticCat cyc:prettyString \"domestic cat\" .\n"
            + "cyc:DomesticCat cyc:prettyString \"domestic cats\" .\n"
            + "cyc:DomesticCat cyc:prettyString \"house cats\" .\n"
            + "cyc:DomesticCat cyc:prettyString \"pussies\" .\n"
            + "cyc:DomesticCat cyc:prettyString \"pussy\" .\n"
            + "cyc:DomesticCat cyc:prettyString-Canonical \"house cat\" .\n"
            + "cyc:DomesticCat rdf:type cyc:DomesticatedAnimalType .\n"
            + "cyc:DomesticCat rdf:type cyc:OrganismClassificationType .\n"
            + "cyc:DomesticCat rdfs:comment \"Cats people commonly keep as pets\" .\n"
            + "cyc:DomesticCat rdfs:subClassOf cyc:Cat .\n"
            + "cyc:DomesticCat rdfs:subClassOf cyc:DomesticPet .\n", result.toString());
    assertTrue(result instanceof ClassKBObject);
  }

  /**
   * Test of persistKBObject method, of class KBAccess.
   */
  @Test
  public void testPersistKBObject() {
    LOGGER.info("persistKBObject");
    final KBAccess instance = new KBAccess(rdfEntityManager);
    final AbstractKBObject kbObject = instance.findKBObject(OPEN_CYC, new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat"));
    instance.persistKBObject(OPEN_CYC, kbObject);
    final ClassKBObject loadedKBObject = (ClassKBObject) instance.findKBObject(OPEN_CYC, new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat"));
    assertEquals(kbObject, loadedKBObject);
  }

  /**
   * Test of getRestrictions method, of class KBAccess.
   */
  @Test
  public void testGetRestrictions_String_URI() {
    LOGGER.info("getRestrictions");
    URI predicate = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final KBAccess instance = new KBAccess(rdfEntityManager);
    Set<AbstractRestriction> result = instance.getRestrictionsByPredicate(OPEN_CYC, predicate);
    assertEquals("[[Restriction on cyc:performedBy, someVauesFrom cyc:MaleHuman], [Restriction on cyc:performedBy, allVauesFrom cyc:Agent-Generic]]", result.toString());
  }

  /**
   * Test of getRestrictions method, of class KBAccess.
   */
  @Test
  public void testGetRestrictions_3args() {
    LOGGER.info("getRestrictions");
    URI subject = new URIImpl(Constants.CYC_NAMESPACE + "Action");
    URI predicate = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final KBAccess instance = new KBAccess(rdfEntityManager);
    Set<AbstractRestriction> result = instance.getRestrictions(OPEN_CYC, subject, predicate);
    assertEquals("[[Restriction on cyc:performedBy, allVauesFrom cyc:Agent-Generic]]", result.toString());
    result = instance.getRestrictions(
            OPEN_CYC,
            new URIImpl(Constants.CYC_NAMESPACE + "Snowboarding"),
            predicate);
    assertEquals("[[Restriction on cyc:performedBy, someVauesFrom cyc:MaleHuman], [Restriction on cyc:performedBy, allVauesFrom cyc:Agent-Generic]]", result.toString());
    result = instance.getRestrictions(
            OPEN_CYC,
            new URIImpl(Constants.CYC_NAMESPACE + "DeckOfCards"),
            new URIImpl(Constants.CYC_NAMESPACE + "parts"));
    assertEquals("[[Restriction on cyc:parts, cardinality 52]]", result.toString());
    result = instance.getRestrictions(
            OPEN_CYC,
            new URIImpl(Constants.CYC_NAMESPACE + "Person"),
            new URIImpl(Constants.CYC_NAMESPACE + "fosterFather"));
    assertEquals("[[Restriction on cyc:fosterFather, minCardinality 0]]", result.toString());
    result = instance.getRestrictions(
            OPEN_CYC,
            new URIImpl(Constants.CYC_NAMESPACE + "Person"),
            new URIImpl(Constants.CYC_NAMESPACE + "biologicalFather"));
    assertEquals("[[Restriction on cyc:biologicalFather, maxCardinality 1]]", result.toString());
  }

  /**
   * Tests of restriction adding and removing methods, of class KBAccess.
   */
  @Test
  public void testAddingAndRemovingRestrictions() {
    LOGGER.info("adding and removing restrictions");
    final KBAccess instance = new KBAccess(rdfEntityManager);

    final URI subject = new URIImpl(Constants.CYC_NAMESPACE + "GeographicalRegion");
    final URI predicate = new URIImpl(Constants.CYC_NAMESPACE + "nearbyTheaters");

    // clear testing state in the otherwise non-cleared repository
    final Set<AbstractRestriction> existingRestrictions = new HashSet<AbstractRestriction>();
    existingRestrictions.addAll(instance.getRestrictionsByPredicate(OPEN_CYC, predicate));
    for (final AbstractRestriction restriction : existingRestrictions) {
      instance.removeRestriction(OPEN_CYC, restriction, subject);
      rdfEntityManager.remove(restriction, OPEN_CYC);
    }
    assertEquals("[]", instance.getRestrictions(OPEN_CYC, subject, predicate).toString());
    assertEquals("[]", instance.getRestrictionsByPredicate(OPEN_CYC, predicate).toString());

    // add an all-values-from restriction
    final AbstractRestriction allValuesFromRestriction = new AllValuesFromRestriction(
            predicate,
            new URIImpl(Constants.CYC_NAMESPACE + "MovieTheaterSpace"));
    rdfEntityManager.persist(allValuesFromRestriction, OPEN_CYC);
    instance.addRestriction(OPEN_CYC, allValuesFromRestriction, subject);
    assertEquals("[[Restriction on cyc:nearbyTheaters, allVauesFrom cyc:MovieTheaterSpace]]", instance.getRestrictions(OPEN_CYC, subject, predicate).toString());
    assertEquals("[[Restriction on cyc:nearbyTheaters, allVauesFrom cyc:MovieTheaterSpace]]", instance.getRestrictionsByPredicate(OPEN_CYC, predicate).toString());

    // add a some-values-from restriction
    final AbstractRestriction someValuesFromRestriction = new SomeValuesFromRestriction(
            predicate,
            new URIImpl(Constants.CYC_NAMESPACE + "DriveInTheater"));
    rdfEntityManager.persist(someValuesFromRestriction, OPEN_CYC);
    instance.addRestriction(OPEN_CYC, someValuesFromRestriction, subject);
    assertEquals("[[Restriction on cyc:nearbyTheaters, someVauesFrom cyc:DriveInTheater], [Restriction on cyc:nearbyTheaters, allVauesFrom cyc:MovieTheaterSpace]]", instance.getRestrictions(OPEN_CYC, subject, predicate).toString());
    assertEquals("[[Restriction on cyc:nearbyTheaters, someVauesFrom cyc:DriveInTheater], [Restriction on cyc:nearbyTheaters, allVauesFrom cyc:MovieTheaterSpace]]", instance.getRestrictionsByPredicate(OPEN_CYC, predicate).toString());

    // add has-value restriction
    final AbstractRestriction hasValueRestriction = new HasValueRestriction(
            predicate,
            new URIImpl(Constants.CYC_NAMESPACE + "DobieTheater"));
    rdfEntityManager.persist(hasValueRestriction, OPEN_CYC);
    instance.addRestriction(OPEN_CYC, hasValueRestriction, subject);
    assertEquals("[[Restriction on cyc:nearbyTheaters, hasValue cyc:DobieTheater], [Restriction on cyc:nearbyTheaters, someVauesFrom cyc:DriveInTheater], [Restriction on cyc:nearbyTheaters, allVauesFrom cyc:MovieTheaterSpace]]", instance.getRestrictions(OPEN_CYC, subject, predicate).toString());
    assertEquals("[[Restriction on cyc:nearbyTheaters, hasValue cyc:DobieTheater], [Restriction on cyc:nearbyTheaters, someVauesFrom cyc:DriveInTheater], [Restriction on cyc:nearbyTheaters, allVauesFrom cyc:MovieTheaterSpace]]", instance.getRestrictionsByPredicate(OPEN_CYC, predicate).toString());

    // delete the has-value restriction
    instance.removeRestriction(OPEN_CYC, hasValueRestriction, subject);
    assertEquals("[[Restriction on cyc:nearbyTheaters, someVauesFrom cyc:DriveInTheater], [Restriction on cyc:nearbyTheaters, allVauesFrom cyc:MovieTheaterSpace]]", instance.getRestrictions(OPEN_CYC, subject, predicate).toString());
    assertEquals("[[Restriction on cyc:nearbyTheaters, someVauesFrom cyc:DriveInTheater], [Restriction on cyc:nearbyTheaters, allVauesFrom cyc:MovieTheaterSpace]]", instance.getRestrictionsByPredicate(OPEN_CYC, predicate).toString());

    // delete the some-values-from restriction
    instance.removeRestriction(OPEN_CYC, someValuesFromRestriction, subject);
    assertEquals("[[Restriction on cyc:nearbyTheaters, allVauesFrom cyc:MovieTheaterSpace]]", instance.getRestrictions(OPEN_CYC, subject, predicate).toString());
    assertEquals("[[Restriction on cyc:nearbyTheaters, allVauesFrom cyc:MovieTheaterSpace]]", instance.getRestrictionsByPredicate(OPEN_CYC, predicate).toString());

    // delete the all-values-from restriction
    instance.removeRestriction(OPEN_CYC, allValuesFromRestriction, subject);
    assertEquals("[]", instance.getRestrictions(OPEN_CYC, subject, predicate).toString());
    assertEquals("[]", instance.getRestrictionsByPredicate(OPEN_CYC, predicate).toString());

    // add a cardinality restriction
    final AbstractRestriction cardinalityRestriction = new CardinalityRestriction(
            predicate,
            1000);
    rdfEntityManager.persist(cardinalityRestriction, OPEN_CYC);
    instance.addRestriction(OPEN_CYC, cardinalityRestriction, subject);
    assertEquals("[[Restriction on cyc:nearbyTheaters, cardinality 1000]]", instance.getRestrictions(OPEN_CYC, subject, predicate).toString());
    assertEquals("[[Restriction on cyc:nearbyTheaters, cardinality 1000]]", instance.getRestrictionsByPredicate(OPEN_CYC, predicate).toString());

    // delete the cardinality restriction
    instance.removeRestriction(OPEN_CYC, cardinalityRestriction, subject);
    assertEquals("[]", instance.getRestrictions(OPEN_CYC, subject, predicate).toString());
    assertEquals("[]", instance.getRestrictionsByPredicate(OPEN_CYC, predicate).toString());

    // add a minimum cardinality restriction
    final AbstractRestriction minCardinalityRestriction = new MinCardinalityRestriction(
            predicate,
            99);
    rdfEntityManager.persist(minCardinalityRestriction, OPEN_CYC);
    instance.addRestriction(OPEN_CYC, minCardinalityRestriction, subject);
    assertEquals("[[Restriction on cyc:nearbyTheaters, minCardinality 99]]", instance.getRestrictions(OPEN_CYC, subject, predicate).toString());
    assertEquals("[[Restriction on cyc:nearbyTheaters, minCardinality 99]]", instance.getRestrictionsByPredicate(OPEN_CYC, predicate).toString());

    // delete the minimum cardinality restriction
    instance.removeRestriction(OPEN_CYC, minCardinalityRestriction, subject);
    assertEquals("[]", instance.getRestrictions(OPEN_CYC, subject, predicate).toString());
    assertEquals("[]", instance.getRestrictionsByPredicate(OPEN_CYC, predicate).toString());

    // add a maximum cardinality restriction
    final AbstractRestriction maxCardinalityRestriction = new MaxCardinalityRestriction(
            predicate,
            1001);
    rdfEntityManager.persist(maxCardinalityRestriction, OPEN_CYC);
    instance.addRestriction(OPEN_CYC, maxCardinalityRestriction, subject);
    assertEquals("[[Restriction on cyc:nearbyTheaters, maxCardinality 1001]]", instance.getRestrictions(OPEN_CYC, subject, predicate).toString());
    assertEquals("[[Restriction on cyc:nearbyTheaters, maxCardinality 1001]]", instance.getRestrictionsByPredicate(OPEN_CYC, predicate).toString());

    // delete the maximum cardinality restriction
    instance.removeRestriction(OPEN_CYC, maxCardinalityRestriction, subject);
    assertEquals("[]", instance.getRestrictions(OPEN_CYC, subject, predicate).toString());
    assertEquals("[]", instance.getRestrictionsByPredicate(OPEN_CYC, predicate).toString());

  }

  /**
   * Test of doesTermExist method, of class KBAccess.
   */
  @Test
  public void testDoesTermExist() {
    LOGGER.info("doesTermExist");
    final KBAccess instance = new KBAccess(rdfEntityManager);
    assertTrue(instance.doesTermExist(OPEN_CYC, new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat")));
    assertFalse(instance.doesTermExist(OPEN_CYC, new URIImpl(Constants.CYC_NAMESPACE + "not-a-term")));
  }

}
