/*
 * LoadAFriend.java
 *
 * Created on December 19, 2006, 10:22 PM
 *
 * Description: Loads a Friend instance.
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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.util.TexaiException;

/** Sample that loads a Friend instance.
 *
 * @author reed
 */
public final class LoadAFriend {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(PersistANewFriend.class.getName());
  /** the RDF entity manager */
  private final RDFEntityManager rdfEntityManager;
  /** the repository connection */
  private RepositoryConnection repositoryConnection;
  /** the friend entity id */
  private URI friendId;

  /**
   * Creates a new instance of LoadAFriend.
   */
  public LoadAFriend() {
    super();
    rdfEntityManager = new RDFEntityManager();
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
    friendId = friend.getId();
    try {
      repositoryConnection.commit();
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Loads a persisted Friend instance. */
  public void loadAPersistedFriend() {
    // load via an iterator
    final Iterator<Friend> friend_iter = rdfEntityManager.rdfEntityIterator(
            Friend.class,
            null);  // overrideContext
    final Friend friend1 = friend_iter.next();
    LOGGER.info("loaded via iterator: " + friend1);

    // load via friend id
    final Friend friend2 = rdfEntityManager.find(
            Friend.class,
            friendId);
    LOGGER.info("loaded via URI: " + friendId + " --> " + friend2);

    // load via identifying property value
    final URI predicate = new URIImpl("http://xmlns.com/foaf/0.1/name");
    final Value value = new LiteralImpl("Stephen Reed");
    final List<Friend> friends = rdfEntityManager.find(
            predicate,
            value,
            Friend.class);
    assert friends.size() == 1 : "friends must have size 1";
    final Friend friend3 = friends.get(0);
    LOGGER.info("loaded via an identifying property: " + value.toString() + " --> " + friend3);
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
    final LoadAFriend loadAFriend = new LoadAFriend();
    loadAFriend.initialize();
    loadAFriend.createAndPersistAFriend();
    loadAFriend.loadAPersistedFriend();
    loadAFriend.finalization();
  }
}
