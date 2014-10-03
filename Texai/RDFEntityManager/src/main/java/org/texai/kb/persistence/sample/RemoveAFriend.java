/*
 * RemoveAFriend.java
 *
 * Created on December 21, 2006, 12:47 PM
 *
 * Description: Removes a Friend object.
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

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.util.TexaiException;

/** Sample that removes a Friend object.
 *
 * @author reed
 */
public final class RemoveAFriend {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(RemoveAFriend.class.getName());
  /** the RDF entity manager */
  private final RDFEntityManager rdfEntityManager = new RDFEntityManager();
  /** the repository connection */
  private RepositoryConnection repositoryConnection;
  /** the friend entity id */
  private URI friendId;

  /**
   * Creates a new instance of RemoveAFriend.
   */
  public RemoveAFriend() {
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
    // populate the friend
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

    // persist the friend and commit the transaction
    rdfEntityManager.persist(friend);
    friendId = friend.getId();
    try {
      repositoryConnection.commit();
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Removes the friend. */
  public void removeFriend() {
    // load the friend
    Friend friend = rdfEntityManager.find(
            Friend.class,
            friendId);
    LOGGER.info("loaded via URI: " + friendId + " --> " + friend);
    friend.setName("Stephen L. Reed");

    // remove the friend and commit the transaction
    friendId = friend.getId();
    rdfEntityManager.remove(friend);
    try {
      repositoryConnection.commit();
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }

    // attempt to load the friend
    friend = rdfEntityManager.find(
            Friend.class,
            friendId);
    if (friend == null) {
      LOGGER.info("as expected, cannot load a removed entity via URI: " + friendId);
    } else {
      LOGGER.info("error, loaded a removed entity via URI: " + friendId + " --> " + friend);
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
    LOGGER.info("LoadAFriend completed");
  }

  /** Executes this application.
   *
   * @param args the command line arguments, which are not used
   */
  public static void main(final String[] args) {
    final RemoveAFriend deleteAFriend = new RemoveAFriend();
    deleteAFriend.initialize();
    deleteAFriend.createAndPersistAFriend();
    deleteAFriend.removeFriend();
    deleteAFriend.finalization();
  }
}
