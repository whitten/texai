/*
 * DistributedRepositoryManager.java
 *
 * Created on May 18, 2009, 12:48:39 PM
 *
 * Description: Provides a distributed repository manager.
 *
 * Copyright (C) May 18, 2009 Stephen L. Reed.
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
package org.texai.kb.persistence;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.DatatypeConverter;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.ws.jaxme.impl.DatatypeConverterImpl;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.nativerdf.NativeStore;
import org.texai.kb.persistence.domainEntity.RepositoryContentDescription;
import org.texai.kb.persistence.domainEntity.RepositoryContentDescriptionItem;
import org.texai.kb.persistence.parser.ParseException;
import org.texai.kb.persistence.parser.RepositoryContentDescriptionParser;
import org.texai.util.FileSystemUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/** Provides a repository manager.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public final class DistributedRepositoryManager {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(DistributedRepositoryManager.class);
  /** the indicator whether debug logging is enabled */
  private static final boolean IS_DEBUG_LOGGING_ENABLED = LOGGER.isDebugEnabled();
  /** the singleton distributed repository manager instance */
  private static DistributedRepositoryManager distributedRepositoryManager;
  /** the local repository dictionary, repository name --> local repository */
  private final Map<String, Repository> localRepositoryDictionary = new HashMap<>();
  /** the remote repository dictionary, repository name --> remote repository */
  private final Map<String, Repository> remoteRepositoryDictionary = new HashMap<>();
  /** the class name repository dictionary, class name --> repository name */
  private final Map<String, String> classNameRepositoryDictionary = new HashMap<>();
  /** the testing repository name */
  private String testRepositoryName = null;
  /** the repository content description repository */
  private Repository repositoryContentDescriptionRepository;
  /** the initialization lock */
  private static final Object DISTRIBUTED_REPOSITORY_MANAGER_LOCK = new Object();
  /** the not-initialized phase */
  private static final int NOT_INITIALIZED_PHASE = 0;
  /** the initialized from file stage */
  private static final int INITIALIZED_USING_FILE_PHASE = 1;
  /** the initialized from repository stage */
  private static final int INITIALIZED_USING_REPOSITORY_PHASE = 2;
  /** the initialization phase */
  private static int initializationPhase = NOT_INITIALIZED_PHASE;
  /** the repository content descriptions */
  private Set<RepositoryContentDescription> repositoryContentDescriptions;
  /** the repository path dictionary, repository name --> path to data directory */
  private static final Map<String, String> REPOSITORY_PATH_DICTIONARY = new HashMap<>();
  /** the Sesame server host URL, or null to use the local native store */
  private static String sesameServerAddress = null;
  /** the directory in which the production repositories are located */
  private static final String REPOSITORIES_DIRECTORY = System.getenv("REPOSITORIES");
  /** the directory in which the test repositories are located */
  private static final String TEST_REPOSITORIES_DIRECTORY = System.getenv("REPOSITORIES_TMPFS");

  /** Constructs a new DistributedRepositoryManager instance.  */
  public DistributedRepositoryManager() {
  }

  /** Copies the named repository from the production directory to the test directory.
   *
   * @param repositoryName the name of the repository to copy
   */
  public static void copyProductionRepositoryToTest(final String repositoryName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(repositoryName) : "repositoryName must not be empty";
    assert StringUtils.isNonEmptyString(REPOSITORIES_DIRECTORY) : "REPOSITORIES must set to the production repositories directory";
    assert StringUtils.isNonEmptyString(TEST_REPOSITORIES_DIRECTORY) : "REPOSITORIES_TMPFS must set to the test repositories directory, e.g. /mnt/tmpfs/repositories";

    try {
      final String testRepositoryPath = TEST_REPOSITORIES_DIRECTORY + '/' + repositoryName;
      final File testRepositoryDirectory = new File(testRepositoryPath);
      try {
        if (testRepositoryDirectory.exists()) {
          LOGGER.info("cleaning the test repository for " + repositoryName);
          FileUtils.cleanDirectory(testRepositoryDirectory);
        }
      } catch (final IOException ex) {
        throw new TexaiException(ex);
      }

      final String repositoryPath = REPOSITORIES_DIRECTORY + '/' + repositoryName;
      final File repositoryDirectory = new File(repositoryPath);
      LOGGER.info("copying the " + repositoryName + " from production (" + repositoryDirectory.getPath() + ") to test (" + testRepositoryDirectory.getPath() + ")");
      FileUtils.copyDirectory(repositoryDirectory, testRepositoryDirectory);
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Gets the Sesame server host URL.
   *
   * @return the sesameServerAddress the Sesame server host URL, or null to use the local native store
   */
  public static String getSesameServerAddress() {
    return sesameServerAddress;
  }

  /** Sets the Sesame server host URL.
   *
   * @param _sesameServerAddress the Sesame server host URL, or null to use the local native store
   */
  public static void setSesameServerAddress(final String _sesameServerAddress) {
    sesameServerAddress = _sesameServerAddress;
  }

  /** Adds the given repository name and path entry to the repository path dictionary.
   *
   * @param repositoryName the repository name
   * @param repositoryPath the path to the repository's data directory
   */
  public static void addRepositoryPath(final String repositoryName, final String repositoryPath) {
    //Preconditions
    assert StringUtils.isNonEmptyString(repositoryName) : "repositoryName must not be empty";
    assert StringUtils.isNonEmptyString(repositoryPath) : "repositoryPath must not be empty";

    synchronized (REPOSITORY_PATH_DICTIONARY) {
      REPOSITORY_PATH_DICTIONARY.put(repositoryName, repositoryPath);
    }
  }

  /** Adds the given test repository name and its looked-up path entry to the repository path dictionary.
   *
   * @param repositoryName the repository name
   * @param isRepositoryDirectoryCleaned the indicator to clean the the directory containing the given named test repository files
   */
  public static void addTestRepositoryPath(final String repositoryName, final boolean isRepositoryDirectoryCleaned) {
    //Preconditions
    assert StringUtils.isNonEmptyString(repositoryName) : "repositoryName must not be empty";
    assert StringUtils.isNonEmptyString(TEST_REPOSITORIES_DIRECTORY) : "REPOSITORIES_TMPFS must set to the test repositories directory, e.g. /mnt/tmpfs/repositories";

    final String testRepositoryPath = TEST_REPOSITORIES_DIRECTORY + '/' + repositoryName;
    final File testRepositoryDirectory = new File(testRepositoryPath);
    if (isRepositoryDirectoryCleaned) {
      try {
        if (testRepositoryDirectory.exists()) {
          FileUtils.cleanDirectory(testRepositoryDirectory);
        }
      } catch (final IOException ex) {
        throw new TexaiException(ex);
      }
    }
    assert testRepositoryDirectory != null : "testRepositoryDirectory must not be null";

    synchronized (REPOSITORY_PATH_DICTIONARY) {
      REPOSITORY_PATH_DICTIONARY.put(repositoryName, testRepositoryPath);
    }
  }

  /** Gets the singleton distributed repository manager instance, while performing two phase initialization.
   *
   * @return the singleton distributed repository manager instance
   */
  public static synchronized DistributedRepositoryManager getInstance() {
    if (initializationPhase == NOT_INITIALIZED_PHASE) {
      // because the RDF entity manager depends upon an initialized distributed repository manager, it cannot be
      // used to persist or load the repository content description objects
      LOGGER.info("initializing the distributed repository manager using file input");
      synchronized (DISTRIBUTED_REPOSITORY_MANAGER_LOCK) {
        initializationPhase = INITIALIZED_USING_FILE_PHASE;
        assert distributedRepositoryManager == null;
        distributedRepositoryManager = new DistributedRepositoryManager();
        distributedRepositoryManager.initializeUsingFile();
        deleteNamedRepository("RepositoryContentDescriptions");
        RDFEntityManager.setDistributedRepositoryManager(distributedRepositoryManager);  // inject dependency

        // conveniently perform other one-time class initializations
        DatatypeConverter.setDatatypeConverter(new DatatypeConverterImpl());
      }
    } else if (initializationPhase == INITIALIZED_USING_FILE_PHASE) {
      // the distributed repository manager has been initialized from a file, so it can be used by the RDF entity
      // manager to persist the repository content descriptions into a repository
      LOGGER.info("initializing the distributed repository manager by persisting its entities");
      synchronized (DISTRIBUTED_REPOSITORY_MANAGER_LOCK) {
        initializationPhase = INITIALIZED_USING_REPOSITORY_PHASE;
        assert distributedRepositoryManager != null;
        distributedRepositoryManager.initializeUsingRepository();
      }
    }

    //Postconditions
    assert distributedRepositoryManager != null : "distributedRepositoryManager must not be null, initializationPhase: " + initializationPhase;

    return distributedRepositoryManager;
  }

  /** Clears the named repository.
   *
   * @param repositoryName the repository name
   */
  public static void clearNamedRepository(final String repositoryName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(repositoryName) : "repositoryName must not be empty";

    final RepositoryConnection repositoryConnection = getInstance().getRepositoryConnectionForRepositoryName(repositoryName);
    final String repositoryPath = repositoryConnection.getRepository().getDataDir().toString();
    if (repositoryPath.equals(System.getenv("REPOSITORIES") + "/OpenCyc")) {
      throw new TexaiException("attempting to clear the OpenCyc repository");
    }
    try {
      LOGGER.info("clearing the repository: " + repositoryPath);
      repositoryConnection.clear();
      repositoryConnection.close();
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Deletes the named repository.
   *
   * @param repositoryName the repository name
   */
  public static void deleteNamedRepository(final String repositoryName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(repositoryName) : "repositoryName must not be empty";
    assert StringUtils.isNonEmptyString(REPOSITORIES_DIRECTORY) : "REPOSITORIES must set to the production repositories directory";

    String repositoryPath;
    synchronized (REPOSITORY_PATH_DICTIONARY) {
      repositoryPath = REPOSITORY_PATH_DICTIONARY.get(repositoryName);
    }
    if (repositoryPath == null) {
      repositoryPath = REPOSITORIES_DIRECTORY + "/" + repositoryName;
    }
    final File dataDirectory = new File(repositoryPath);
    LOGGER.info("deleting Sesame2 repository in " + dataDirectory.toString());
    FileSystemUtils.deleteRecursively(dataDirectory);
  }

  /** Initializes the repository content descriptions without using the RDF entity manager to load or persist them. */
  private void initializeUsingFile() {
    //Preconditions
    assert StringUtils.isNonEmptyString(REPOSITORIES_DIRECTORY) : "REPOSITORIES must set to the production repositories directory";

    BufferedInputStream bufferedInputStream;
    File repositoryContentDescriptionsFile;
    repositoryContentDescriptionsFile = new File(REPOSITORIES_DIRECTORY + "/repository.rcd");
    assert repositoryContentDescriptionsFile.exists() : REPOSITORIES_DIRECTORY + "/repository.rcd not found";
    try {
      bufferedInputStream = new BufferedInputStream(new FileInputStream(repositoryContentDescriptionsFile));
    } catch (FileNotFoundException ex) {
      assert false;
      bufferedInputStream = null;
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("parsing the repository content descriptions file " + repositoryContentDescriptionsFile.toString());
    }
    final RepositoryContentDescriptionParser repositoryContentDescriptionParser
            = new RepositoryContentDescriptionParser(bufferedInputStream);
    try {
      repositoryContentDescriptions = repositoryContentDescriptionParser.parseInput();
    } catch (final ParseException ex) {
      throw new TexaiException(ex);
    }
    final Iterator<RepositoryContentDescription> repositoryContentDescription_iter = repositoryContentDescriptions.iterator();
    while (repositoryContentDescription_iter.hasNext()) {
      final RepositoryContentDescription repositoryContentDescription = repositoryContentDescription_iter.next();
      if (IS_DEBUG_LOGGING_ENABLED) {
        LOGGER.debug("processing repository content description for: " + repositoryContentDescription);
      }
      final String repositoryName = repositoryContentDescription.getRepositoryName();
      distributedRepositoryManager.addLocalRepository(new LazyRepository(
              repositoryName,
              repositoryContentDescription.getIndices()));
      for (final RepositoryContentDescriptionItem repositoryContentDescriptionItem : repositoryContentDescription.getRepositoryContentDescriptionItems()) {
        final URI classTerm = repositoryContentDescriptionItem.getClassTerm();
        if (classTerm != null) {
          if (IS_DEBUG_LOGGING_ENABLED) {
            LOGGER.debug("  " + classTerm.getLocalName() + " ==> " + repositoryName);
          }
          distributedRepositoryManager.classNameRepositoryDictionary.put(classTerm.getLocalName(), repositoryName);
        }
      }
    }
  }

  /** Persists the repository content descriptions to their containing repository using the RDF entity manager, which in turn
   * depends upon having an initialized distributed repository manager.
   */
  private void initializeUsingRepository() {
    //Preconditions
    assert repositoryContentDescriptions != null : "repositoryContentDescriptions must not be null";

    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    for (final RepositoryContentDescription repositoryContentDescription : repositoryContentDescriptions) {
      LOGGER.info(repositoryContentDescription.getRepositoryName());
      rdfEntityManager.persist(repositoryContentDescription);
    }
    rdfEntityManager.close();
  }

  /** Shuts down the initialized repositories. */
  public static synchronized void shutDown() {
    if (distributedRepositoryManager != null) {
      for (final Repository repository : distributedRepositoryManager.localRepositoryDictionary.values()) {
        if (repository instanceof LazyRepository) {
          final Repository repository1 = ((LazyRepository) repository).getRepository();
          if (repository1 != null) {
            LOGGER.info("shutting down lazy " + repository1.getDataDir());
            final ShutDownRepository shutDownRepository = new ShutDownRepository(repository1);
            final Thread thread = new Thread(shutDownRepository);
            thread.start();
          }
        } else {
          LOGGER.info("shutting down " + repository.getDataDir());
          final ShutDownRepository shutDownRepository = new ShutDownRepository(repository);
          final Thread thread = new Thread(shutDownRepository);
          thread.start();
        }
      }
    }
  }

  /** Shuts down a given repository. */
  private static class ShutDownRepository implements Runnable {

    /** the repository */
    private final Repository repository;

    /** Creates a new ShutDownRepository instance.
     *
     * @param repository the repository
     */
    ShutDownRepository(final Repository repository) {
      this.repository = repository;
    }

    /** Shuts down the repository. */
    @Override
    public void run() {
      try {
        repository.shutDown();
      } catch (final RepositoryException ex) {
        throw new TexaiException(ex);
      }
    }
  }

  /** Gets the local repository dictionary, name --> local repository.
   *
   * @return the local repository dictionary, name --> local repository
   */
  public synchronized Map<String, Repository> getLocalRepositoryDictionary() {
    return localRepositoryDictionary;
  }

  /** Adds a local repository.
   *
   * @param repository the local repository
   */
  public synchronized void addLocalRepository(final Repository repository) {
    //Preconditions
    assert repository != null : "repository must not be null";

    if (repository instanceof LazyRepository) {
      localRepositoryDictionary.put(((LazyRepository) repository).getRepositoryName(), repository);
    } else {
      localRepositoryDictionary.put(repository.getDataDir().getName(), repository);
    }
  }

  /** Gets the remote repository dictionary, name --> remote repository.
   *
   * @return the remote repository dictionary, name --> remote repository
   */
  public synchronized Map<String, Repository> getRemoteRepositoryDictionary() {
    return remoteRepositoryDictionary;
  }

  /** Returns a repository connection corresponding to the given repository name, which the caller must eventually close.
   *
   * @param repositoryName the given repository name.
   * @return a repository connection
   */
  public synchronized RepositoryConnection getRepositoryConnectionForRepositoryName(final String repositoryName) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";

    RepositoryConnection repositoryConnection = null;
    //TODO fix up for remote repositories
    try {
      final Repository repository = localRepositoryDictionary.get(repositoryName);
      if (repository == null) {
        throw new TexaiException("repository not found: " + repositoryName + "\n" + localRepositoryDictionary);
      }
      repositoryConnection = repository.getConnection();
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }

    //Postconditons
    assert repositoryConnection != null : "repositoryConnection must not be null";

    return repositoryConnection;
  }

  /** Gets the testing repository name.
   *
   * @return the testing repository name
   */
  public String getTestRepositoryName() {
    return testRepositoryName;
  }

  /** Sets the testing repository name
   *
   * @param testRepositoryName the testing repository name
   */
  public void setTestRepositoryName(final String testRepositoryName) {
    this.testRepositoryName = testRepositoryName;
  }

  /** Gets the repository content description repository.
   *
   * @return the repository content description repository
   */
  public Repository getRepositoryContentDescriptionRepository() {
    return repositoryContentDescriptionRepository;
  }

  /** Sets the repository content description repository
   *
   * @param repositoryContentDescriptionRepository the repository content description repository
   */
  public void setRepositoryContentDescriptionRepository(final Repository repositoryContentDescriptionRepository) {
    //preconditions
    assert repositoryContentDescriptionRepository != null : "repositoryContentDescriptionRepository must not be null";

    this.repositoryContentDescriptionRepository = repositoryContentDescriptionRepository;
  }

  /** Returns the repository name corresponding to the given URI which identifies an RDF entity.
   *
   * @param uri the given URI which identifies an RDF entity
   * @return the repository name
   */
  public String getRepositoryNameForInstance(final URI uri) {
    //preconditions
    assert uri != null : "uri must not be null";

    if (testRepositoryName == null) {
      // typical case
      final String className = parseClassNameFromURI(uri);
      assert className != null : "cannot find repository name for instance: " + uri;
      return getRepositoryNameForClassName(className);
    } else {
      // testing - a test repository has been added to this distributed repository manager
      return testRepositoryName;
    }
  }

  /** Returns the repository name corresponding to the given RDF entity.
   *
   * @param rdfEntity the RDF entity
   * @return the repository name corresponding to the given RDF entity
   */
  public String getRepositoryNameForInstance(final RDFPersistent rdfEntity) {
    //preconditions
    assert rdfEntity != null : "rdfEntity must not be null";

    return getRepositoryNameForInstance(rdfEntity.getId());
  }

  /** Returns the repository name corresponding to the given RDF entity class.
   *
   * @param clazz the class
   * @return the repository name, or null if not found
   */
  public String getRepositoryNameForClass(final Class<?> clazz) {
    //preconditions
    assert clazz != null : "className clazz not be null";

    return getRepositoryNameForClassName(clazz.getName());
  }

  /** Returns the repository name corresponding to the given RDF entity class name.
   *
   * @param className the class name
   * @return the repository name, or null if not found
   */
  public String getRepositoryNameForClassName(final String className) {
    //preconditions
    assert className != null : "className must not be null";
    assert !className.isEmpty() : "className must not be empty";

    if (className.startsWith("org.texai")) {
      return classNameRepositoryDictionary.get(className);
    } else {
      // handle legacy WordNet IDs
      return classNameRepositoryDictionary.get("org.texai.wordnet.domain.entity." + className);
    }
  }

  /** Logs the class name repository dictionary. */
  public static void logClassNameRepositoryDictionary() {
    if (distributedRepositoryManager == null) {
      return;
    }
    final List<String> orderedClassNames = new ArrayList<>(distributedRepositoryManager.classNameRepositoryDictionary.keySet());
    Collections.sort(orderedClassNames);
    for (final String orderedClassName : orderedClassNames) {
      final String repositoryName = distributedRepositoryManager.classNameRepositoryDictionary.get(orderedClassName);
      LOGGER.info(orderedClassName + " --> " + repositoryName);
    }
  }

  /** Returns the class name parsed from the given URI which identifies an RDF entity.
   *
   * @param uri the given URI which identifies an RDF entity
   * @return the class name parsed from the given URI if it has the format of an RDF entity id, otherwise returns null
   */
  public static String parseClassNameFromURI(final URI uri) {
    //preconditions
    assert uri != null : "uri must not be null";

    final String localName = uri.getLocalName();
    int index = -1;
    for (int i = localName.length() - 1; i >= 0; i--) {
      if (localName.charAt(i) == '_') {
        index = i;
        break;
      }
    }
    if (index == -1) {
      return null;
    } else {
      return localName.substring(0, index);
    }
  }

  /** Provides a repository for lazy initialization. */
  @ThreadSafe
  public static final class LazyRepository implements Repository {

    /** the repository name */
    private final String repositoryName;
    /** the indices */
    private final String indices;
    /** the lazily initialized repository */
    private Repository repository;

    /** Constructs a new LazyRepository instance.
     *
     * @param repositoryName the repository name
     * @param indices the indices
     */
    public LazyRepository(
            final String repositoryName,
            final String indices) {
      //Preconditions
      assert repositoryName != null : "repositoryName must not be null";
      assert !repositoryName.isEmpty() : "repositoryName must not be empty";
      assert indices != null : "indices must not be null";
      assert !indices.isEmpty() : "indices must not be empty";

      this.repositoryName = repositoryName;
      this.indices = indices;
    }

    /** Immediately initializes this repository. */
    private synchronized void immediatelyInitialize() {
      //Preconditions
      assert StringUtils.isNonEmptyString(REPOSITORIES_DIRECTORY) : "REPOSITORIES must set to the production repositories directory";

      LOGGER.info("initializing: " + repositoryName);
      if (repository == null) {
        try {
          if (sesameServerAddress == null) {
            String repositoryPath = REPOSITORY_PATH_DICTIONARY.get(repositoryName);
            if (repositoryPath == null) {
              repositoryPath = REPOSITORIES_DIRECTORY + "/" + repositoryName;
            }
            final File dataDirectory = new File(repositoryPath);
            LOGGER.info("accessing local Sesame2 repository in " + dataDirectory.toString());
            repository = new SailRepository(new NativeStore(dataDirectory, indices));
          } else {
            LOGGER.info("accessing remote Sesame2 repository at " + sesameServerAddress + "/" + repositoryName);
            repository = new HTTPRepository(sesameServerAddress, repositoryName);
            repository.setDataDir(new File(sesameServerAddress + "/" + repositoryName));
            LOGGER.info("remote repository: " + repository);
          }
          repository.initialize();
        } catch (final RepositoryException ex) {
          LOGGER.error("error while initializing: " + repositoryName);
          throw new TexaiException(ex);
        }
      }
    }

    /** Sets the directory where data and logging for this repository is stored.
     *
     * @param dataDir the directory where data for this repository is stored
     */
    @Override
    public synchronized void setDataDir(final File dataDir) {
      throw new UnsupportedOperationException("Not supported.");
    }

    /** Gets the directory where data and logging for this repository is stored.
     *
     * @return the directory where data and logging for this repository is stored
     */
    @Override
    public synchronized File getDataDir() {
      if (repository == null) {
        immediatelyInitialize();
      }
      return repository.getDataDir();
    }

    /** Initializes this repository. A repository needs to be initialized before it can be used.
     *
     * @throws RepositoryException when a repository error occurs
     */
    @Override
    public synchronized void initialize() throws RepositoryException {
      if (repository == null) {
        immediatelyInitialize();
      }
      repository.initialize();
    }

    /** Shuts the repository down, releasing any resources that it keeps hold of.
     * Once shut down, the repository can no longer be used until it is re-initialized.
     *
     * @throws RepositoryException when a repository error occurs
     */
    @Override
    public synchronized void shutDown() throws RepositoryException {
      if (repository == null) {
        immediatelyInitialize();
      }
      repository.shutDown();
    }

    /** Checks whether this repository is writable, i.e. if the data contained in this repository can be changed.
     * The writability of the repository is determined by the writability of the Sail that this repository operates on.
     *
     * @return whether this repository is writable
     * @throws RepositoryException when a repository error occurs
     */
    @Override
    public synchronized boolean isWritable() throws RepositoryException {
      if (repository == null) {
        immediatelyInitialize();
      }
      return repository.isWritable();
    }

    /** Opens a connection to this repository that can be used for querying and updating the contents of the repository.
     * Created connections need to be closed to make sure that any resources they keep hold of are released.
     *
     * @return a repository connection
     * @throws RepositoryException when a repository error occurs
     */
    @Override
    public synchronized RepositoryConnection getConnection() throws RepositoryException {
      if (repository == null) {
        immediatelyInitialize();
      }
      return repository.getConnection();
    }

    /** Gets a ValueFactory for this Repository.
     *
     * @return a ValueFactory for this Repository
     */
    @Override
    public synchronized ValueFactory getValueFactory() {
      if (repository == null) {
        immediatelyInitialize();
      }
      return repository.getValueFactory();
    }

    /** Gets the repository name.
     *
     * @return the repositoryName
     */
    public synchronized String getRepositoryName() {
      return repositoryName;
    }

    /** Gets the indices.
     *
     * @return the indices
     */
    public synchronized String getIndices() {
      return indices;
    }

    /** Gets the lazily initialized repository.
     *
     * @return the repository
     */
    public synchronized Repository getRepository() {
      return repository;
    }
  }
}
