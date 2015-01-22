/*
 * RestrictionInitializer.java
 *
 * Created on Nov 12, 2010, 2:25:37 PM
 *
 * Description: Performs initialization of property restrictions.
 *
 * Copyright (C) Nov 12, 2010, Stephen L. Reed.
 */
package org.texai.kb.restriction;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.kb.persistence.KBAccess;
import org.texai.kb.persistence.RDFEntityManager;

/** Performs initialization of property restrictions.
 *
 * @author reed
 */
@NotThreadSafe
public class RestrictionInitializer {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(RestrictionInitializer.class);
  /** the RDF entity manager */
  private final RDFEntityManager rdfEntityManager = new RDFEntityManager();
  /** the OpenCyc repository */
  private static final String OPEN_CYC = "OpenCyc";
  /** the KBAccess object */
  private final KBAccess kbAccess = new KBAccess(rdfEntityManager);

  /** Constructs a new RestrictionInitializer instance. */
  public RestrictionInitializer() {
  }

  /** Initializes this application. */
  private void initialization() {
  }

  /** Processes this application. */
  private void process() {

  }

  /** Closes this application and releases its resources. */
  private void finalization() {
    LOGGER.info("RestrictionInitializer complete");
    rdfEntityManager.close();
  }

  /** Executes this application.
   *
   * @param args the command-line arguments (unused)
   */
  public static void main(final String[] args) {
    final RestrictionInitializer restrictionInitializer = new RestrictionInitializer();
    restrictionInitializer.initialization();
    restrictionInitializer.process();
    restrictionInitializer.finalization();
  }
}
