/*
 * PersistANewFriend.java
 *
 * Created on December 19, 2006, 4:24 PM
 *
 * Description: Persists a new Friend instance.
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
package org.texai.kb.persistence.sample;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.rdfxml.util.RDFXMLPrettyWriter;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.util.TexaiException;

/** Sample that persists a new Friend instance.
 *
 * @author reed
 */
public final class PersistANewFriend {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(PersistANewFriend.class.getName());
  /** the RDF entity manager */
  private final RDFEntityManager rdfEntityManager = new RDFEntityManager();
  /** the repository connection */
  private RepositoryConnection repositoryConnection;

  /**
   * Creates a new instance of PersistANewFriend.
   */
  public PersistANewFriend() {
    super();
  }

  /** Initializes this application. */
  public void initialize() {
    CacheInitializer.initializeCaches();
    getClass().getClassLoader().setDefaultAssertionStatus(true);   // optional
    repositoryConnection = DistributedRepositoryManager.getInstance().getRepositoryConnectionForRepositoryName("Test");
    LOGGER.info("clearing the Sesame2 repository " + repositoryConnection.getRepository().getDataDir());
    try {
      repositoryConnection.clear();
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Creates and persists a Friend instance. */
  public void createAndPersistAFriend() {
    try {
      repositoryConnection.setAutoCommit(false);
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
    // populate the Friend
    final String name = "Stephen Reed";
    final String birthday = "09-20";
    final String gender = "male";
    final Set<Object> thingsMade = new HashSet<>();
    thingsMade.add(new URIImpl("http://texai.org"));
    final String firstName = "Stephen";
    final String familyName = "Reed";
    final Date dateOfBirth = (new GregorianCalendar(1951, 9, 20, 0, 0, 0)).getTime();
    final Friend friend = new Friend(
            name,
            birthday,
            gender,
            thingsMade,
            firstName,
            familyName,
            dateOfBirth);

    // persist the RDF entity and commit the transaction
    rdfEntityManager.persist(friend);
    try {
      repositoryConnection.commit();
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }

    // export the respository in RDF
    try {
      final String rdfOutputFilename = System.getProperties().getProperty("user.home") + "/friend.rdf";
      final FileOutputStream fileOutputStream = new FileOutputStream(rdfOutputFilename);
      final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
      final RDFXMLPrettyWriter rdfXMLPrettyWriter = new RDFXMLPrettyWriter(bufferedOutputStream);
      repositoryConnection.export(rdfXMLPrettyWriter);
    } catch (final RepositoryException | RDFHandlerException | FileNotFoundException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Finalizes this application. */
  public void finalization() {
    CacheManager.getInstance().shutdown();
    rdfEntityManager.close();
    try {
      repositoryConnection.close();
      DistributedRepositoryManager.shutDown();
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
    LOGGER.info("PersistANewFriend completed");
  }

  /** Executes this application.
   *
   * @param args the command line arguments, which are not used
   */
  public static void main(final String[] args) {
    final PersistANewFriend persistANewFriend = new PersistANewFriend();
    persistANewFriend.initialize();
    persistANewFriend.createAndPersistAFriend();
    persistANewFriend.finalization();
  }
}
