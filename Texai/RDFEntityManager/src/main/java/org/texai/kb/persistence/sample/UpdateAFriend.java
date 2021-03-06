/*
 * updateFriend.java
 *
 * Created on December 21, 2006, 12:47 PM
 *
 * Description: Updates a persisted Friend object.
 *
 * Copyright (C) 2006 Stephen L. Reed.
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

/** Sample that updates a persisted Friend object.
 *
 * @author reed
 */
public final class UpdateAFriend {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(UpdateAFriend.class.getName());
  /** the RDF entity manager */
  private final RDFEntityManager rdfEntityManager = new RDFEntityManager();
  /** the repository connection */
  private RepositoryConnection repositoryConnection;
  /** the friend entity id */
  private URI friendId;

  /**
   * Creates a new instance of updateFriend.
   */
  public UpdateAFriend() {
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

  /** Loads, updates, persists and reloads the friend. */
  public void updateFriend() {
    // load the friend
    Friend friend = rdfEntityManager.find(
            Friend.class,
            friendId);
    LOGGER.info("loaded via URI: " + friendId + " --> " + friend);
    friend.setName("Stephen L. Reed");

    // persist the friend and commit the transaction
    rdfEntityManager.persist(friend);
    friendId = friend.getId();
    try {
      repositoryConnection.commit();
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }

    // load the friend
    friend = rdfEntityManager.find(
            Friend.class,
            friendId);
    LOGGER.info("reloaded via URI: " + friendId + " --> " + friend);
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
    final UpdateAFriend updateAFriend = new UpdateAFriend();
    updateAFriend.initialize();
    updateAFriend.createAndPersistAFriend();
    updateAFriend.updateFriend();
    updateAFriend.finalization();
  }
}
