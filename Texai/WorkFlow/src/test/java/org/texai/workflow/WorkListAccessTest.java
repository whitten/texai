/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.workflow;

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
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.workflow.domainEntity.WorkList;

/**
 *
 * @author reed
 */
public class WorkListAccessTest {

  /** the RDF entity manager */
  private static final RDFEntityManager rdfEntityManager = new RDFEntityManager();
  /** the nouns missing plural worklist purpose term */
  private static final URI NOUNS_MISSING_PLURAL_WORKLIST = new URIImpl(Constants.TEXAI_NAMESPACE + "NounsMissingPlural_WorkList");

  public WorkListAccessTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addRepositoryPath(
            "WorkFlow",
            System.getenv("REPOSITORIES_TMPFS") + "/WorkFlow");
    DistributedRepositoryManager.clearNamedRepository("WorkFlow");
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    rdfEntityManager.close();
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of createWorkList method, of class WorkListAccess.
   */
  @Test
  public void findOrCreateWorkList() {
    System.out.println("findOrCreateWorkList");
    WorkListAccess instance = new WorkListAccess(rdfEntityManager);
    WorkList result = instance.findWorkList(NOUNS_MISSING_PLURAL_WORKLIST);
    assertNull(result);
    result = instance.findOrCreateWorkList(NOUNS_MISSING_PLURAL_WORKLIST);
    assertEquals("[WorkList texai:NounsMissingPlural_WorkList size: 0]", result.toString());
    result = instance.findWorkList(NOUNS_MISSING_PLURAL_WORKLIST);
    assertEquals("[WorkList texai:NounsMissingPlural_WorkList size: 0]", result.toString());
  }

}
