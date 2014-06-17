/*
 * FixOpenCycProperty.java
 *
 * Created on Apr 8, 2009, 3:17:59 PM
 *
 * Description: Fixes the Property term in the OpenCyc KB.
 *
 * Copyright (C) Apr 8, 2009 Stephen L. Reed.
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
package org.texai.kb.fix;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.KBAccess;
import org.texai.kb.persistence.RDFEntityManager;

/** Fixes the Property term in the OpenCyc KB.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public final class FixOpenCycProperty {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(FixOpenCycProperty.class);
  /** the bad Property URI */
  private static final URI BAD_PROPERTY_URI = new URIImpl("http://www.w3.org/2000/01/rdf-schema#Property");
  /** the RDF entity manager */
  private final RDFEntityManager rdfEntityManager = new RDFEntityManager();

  /** Constructs a new FixOpenCycProperty instance. */
  public FixOpenCycProperty() {
  }

  /** Initializes this application. */
  public void initialization() {
      CacheInitializer.initializeCaches();
  }

  /** Fixes the Property term in the OpenCyc KB. */
  public void process() {
    Logger.getLogger(KBAccess.class).setLevel(Level.DEBUG);
    final KBAccess kbAccess = new KBAccess(rdfEntityManager);
    kbAccess.renameURI("OpenCyc", BAD_PROPERTY_URI, RDF.PROPERTY);
  }

  /** Finalizes this application and releases its resources. */
  public void finalization() {
    rdfEntityManager.close();
    DistributedRepositoryManager.shutDown();
    LOGGER.info("FixOpenCycProperty completed");
  }

  /** Executes this application.
   *
   * @param args the command line arguments (not used)
   */
  public static void main(final String[] args) {
    final FixOpenCycProperty fixOpenCycProperty = new FixOpenCycProperty();
    fixOpenCycProperty.initialization();
    fixOpenCycProperty.process();
    fixOpenCycProperty.finalization();
    System.exit(0);
  }
}
