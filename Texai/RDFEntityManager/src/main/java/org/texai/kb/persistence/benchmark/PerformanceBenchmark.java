/*
 * PerformanceBenchmark.java
 *
 * Created on August 10, 2007, 12:32 PM
 *
 * Description: Provides a performance test for the RDF Entity Manager.
 *
 * Copyright (C) August 10, 2007 Stephen L. Reed.
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
package org.texai.kb.persistence.benchmark;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;
import org.texai.kb.persistence.RDFUtility;
import org.texai.util.TexaiException;

/** Provides a performance benchmark for measuring the rate at which test RDF entities can be loaded from a Sesame RDF store.
 *
 * @author reed
 */
@NotThreadSafe
public final class PerformanceBenchmark {

  /** the number of reading threads */
  private static final int NBR_THREADS = Runtime.getRuntime().availableProcessors();
  //private static final int NBR_THREADS = 1;
  /** the indicator whether to clear and re-populate the repository */
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PerformanceBenchmark.class);
  /** the RDF entity manager */
  private RDFEntityManager rdfEntityManager = null;
  /** the number of linked RDF test entities to create */
  private int nbrRDFTestEntitiesToCreate = 20000;
  /** the number of linked RDF test entities to randomly read */
  private int nbrRDFTestEntitiesToRead = 40000;
  /** the array of RDF test entity identifying URIs */
  private URI[] rdfTestEntityURIs = new URI[nbrRDFTestEntitiesToCreate];
  /** the executor */
  private final ExecutorService executor;

  /** Creates a new instance of PerformanceBenchmark. */
  public PerformanceBenchmark() {
    executor = Executors.newFixedThreadPool(NBR_THREADS);
  }

  /** Initializes the application. */
  public void initialization() {
    CacheInitializer.initializeCaches();
    getClass().getClassLoader().setDefaultAssertionStatus(true);
    DistributedRepositoryManager.clearNamedRepository("Benchmark");
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.OFF);
    Logger.getLogger(RDFUtility.class).setLevel(Level.OFF);
    rdfEntityManager = new RDFEntityManager();
  }

  /** Finalizes the application. */
  public void finalization() {
    CacheManager.getInstance().shutdown();
    executor.shutdown();
    LOGGER.info("closing the RDF entity manager");
    rdfEntityManager.close();
    LOGGER.info("shutting down the Sesame2 repositories");
    DistributedRepositoryManager.shutDown();
    LOGGER.info("PerformanceBenchmark completed");
  }

  /** Creates linked RDF test entities. */
  public void createLinkedRDFTestEntities() {
    LOGGER.info("creating " + nbrRDFTestEntitiesToCreate + " linked RDFTestEntity instances");
    final int nbrOfLinkedRDFTestEntityPairs = nbrRDFTestEntitiesToCreate / 2;
    final long startMillis = System.currentTimeMillis();
    for (int i = 1; i <= nbrOfLinkedRDFTestEntityPairs; i++) {
      createRDFTestEntity(i);
    }
    long secondsDuration = (System.currentTimeMillis() - startMillis) / 1000;
    if (secondsDuration == 0) {
      secondsDuration = 1;
    }
    LOGGER.info("created " + nbrRDFTestEntitiesToCreate + " at the rate of " + nbrRDFTestEntitiesToCreate / secondsDuration + " per second");
  }

  /** Queries the instance URIs. */
  public void queryInstanceURIs() {
    LOGGER.info("querying the instance URIs");
    try {
      final RepositoryConnection queryRepositoryConnection = DistributedRepositoryManager.getInstance().getRepositoryConnectionForRepositoryName("Benchmark");
      final String queryString =
              "SELECT s FROM {s} rdf:type {<http://texai.org/texai/org.texai.kb.persistence.benchmark.RDFTestEntity>}";
      LOGGER.info("query " + queryString);
      final TupleQuery subjectsTupleQuery = queryRepositoryConnection.prepareTupleQuery(QueryLanguage.SERQL, queryString);
      final List<URI> instanceURIs = new ArrayList<>();
      final TupleQueryResult tupleQueryResult = subjectsTupleQuery.evaluate();
      while (tupleQueryResult.hasNext()) {
        instanceURIs.add((URI) tupleQueryResult.next().getBinding("s").getValue());
      }
      tupleQueryResult.close();
      LOGGER.info("closing the query repository connection");
      queryRepositoryConnection.close();
      if (instanceURIs.isEmpty()) {
        throw new TexaiException("no test entities selected");
      }
      rdfTestEntityURIs = new URI[instanceURIs.size()];
      rdfTestEntityURIs = instanceURIs.toArray(rdfTestEntityURIs);
    } catch (final MalformedQueryException ex) {
      throw new TexaiException(ex);
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
    LOGGER.info("Found " + rdfTestEntityURIs.length + " instance URIs");
  }

  /** Randomly reads linked RDF test entities. */
  public void readLinkedRDFTestEntities() {
    LOGGER.info("randomly reading " + nbrRDFTestEntitiesToRead);
    final long startMillis = System.currentTimeMillis();
    final CountDownLatch doneSignal = new CountDownLatch(NBR_THREADS);
    for (int i = 0; i < NBR_THREADS; i++) {
      executor.execute(new LinkedRDFTestEntityReaderRunnable(doneSignal, i + 1));
    }
    try {
      doneSignal.await();
    } catch (InterruptedException ex) {
      throw new TexaiException(ex);
    }
    double secondsDuration = (float) ((System.currentTimeMillis() - startMillis)) / 1000.0d;
    if (secondsDuration == 0) {
      secondsDuration = 1;
    }
    LOGGER.info("read " + nbrRDFTestEntitiesToRead + " at the rate of " + nbrRDFTestEntitiesToRead / secondsDuration + " per second");
  }

  /** Gets the number of linked RDF test entities to create.
   *
   * @return the nbrRDFTestEntitiesToCreate
   */
  public int getNbrRDFTestEntitiesToCreate() {
    return nbrRDFTestEntitiesToCreate;
  }

  /** Sets the number of linked RDF test entities to create.
   *
   * @param nbrRDFTestEntitiesToCreate the nbrRDFTestEntitiesToCreate to set
   */
  public void setNbrRDFTestEntitiesToCreate(final int nbrRDFTestEntitiesToCreate) {
    this.nbrRDFTestEntitiesToCreate = nbrRDFTestEntitiesToCreate;
  }

  /** Gets the number of linked RDF test entities to randomly read.
   *
   * @return the nbrRDFTestEntitiesToRead
   */
  public int getNbrRDFTestEntitiesToRead() {
    return nbrRDFTestEntitiesToRead;
  }

  /** Sets the number of linked RDF test entities to randomly read.
   *
   * @param nbrRDFTestEntitiesToRead the nbrRDFTestEntitiesToRead to set
   */
  public void setNbrRDFTestEntitiesToRead(final int nbrRDFTestEntitiesToRead) {
    this.nbrRDFTestEntitiesToRead = nbrRDFTestEntitiesToRead;
  }

  /** A parallel runnable that loads random entity URIs. */
  @Immutable
  class LinkedRDFTestEntityReaderRunnable implements Runnable {

    /** the count down latch that synchronizes the calling thread */
    private final CountDownLatch doneSignal;
    /** the thread id */
    private final int threadID;

    /** Constructs a new LinkedRDFTestEntityReaderRunnable instance.
     *
     * @param doneSignal the count down latch that synchronizes the calling thread
     * @param threadID the identification for this runnable
     */
    public LinkedRDFTestEntityReaderRunnable(final CountDownLatch doneSignal, final int threadID) {
      //Preconditions
      assert doneSignal != null : "doneSignal must not be null";

      this.doneSignal = doneSignal;
      this.threadID = threadID;
    }

    /** Executes this thread. */
    @Override
    public void run() {
      final Random random = new Random();
      final RDFEntityManager threadRDFEntityManager = new RDFEntityManager();
      LOGGER.info("starting " + threadID);
      Thread.currentThread().setName("reader " + threadID);
      int nbrTestEntitiesRead = 0;
      for (int i = 1; i < (getNbrRDFTestEntitiesToRead() / NBR_THREADS); i++) {
        nbrTestEntitiesRead++;
        final URI randomURI = rdfTestEntityURIs[random.nextInt(rdfTestEntityURIs.length - 1)];
        final RDFTestEntity randomRDFTestEntity = threadRDFEntityManager.find(
                RDFTestEntity.class,
                randomURI);
        verifyRDFTestEntity(randomRDFTestEntity);
        if (i % 10000 == 0) {
          LOGGER.info(i + " --> " + randomRDFTestEntity.getName() + "  thread " + threadID);
          LOGGER.info("    cache" + CacheManager.getInstance().getCache(Constants.CACHE_CONNECTED_RDF_ENTITY_URIS).getStatistics().toString());
          CacheInitializer.resetCaches();
        }
      }
      LOGGER.info("thread " + threadID + " read " + nbrTestEntitiesRead + " entities");
      threadRDFEntityManager.close();
      doneSignal.countDown();
    }
  }

  /** Creates two linked RDF test entities with the given serial number suffix.
   *
   * @param serialNbr the given serial number suffix
   */
  private void createRDFTestEntity(final int serialNbr) {
    //preconditions
    assert serialNbr > 0 : "serialNbr must be positive";

    final RDFTestEntity rdfTestEntity1 = new RDFTestEntity();
    final RDFTestEntity rdfTestEntity2 = new RDFTestEntity();
    rdfTestEntity1.setDontCareField("do not care");
    rdfTestEntity1.setFavoriteTestRDFEntityPeer(rdfTestEntity2);
    rdfTestEntity1.setMaxNbrOfScooterRiders(2);
    List<RDFTestEntity> myPeers = new ArrayList<>(1);
    myPeers.add(rdfTestEntity2);
    rdfTestEntity1.setMyPeers(myPeers);
    rdfTestEntity1.setName("TestDomainEntity " + serialNbr);
    rdfTestEntity1.setNumberOfCrew(1);
    final String[] comments1 = {"comment 1", "comment 2"};
    rdfTestEntity1.setComment(comments1);
    final Set<String> cyclistNotes = new HashSet<>();
    cyclistNotes.add("note 1");
    cyclistNotes.add("note 2");
    rdfTestEntity2.setDontCareField("do not care");
    rdfTestEntity2.setFavoriteTestRDFEntityPeer(rdfTestEntity2);
    rdfTestEntity2.setMaxNbrOfScooterRiders(2);
    myPeers = new ArrayList<>(1);
    myPeers.add(rdfTestEntity1);
    rdfTestEntity2.setMyPeers(myPeers);
    final List<Double> myPeersStrengths = new ArrayList<>();
    myPeersStrengths.add(Double.valueOf(0.5d));
    rdfTestEntity2.setName("LinkedTestDomainEntity " + serialNbr);
    rdfTestEntity2.setNumberOfCrew(1);
    final String[] comments2 = {"comment 1", "comment 2"};
    rdfTestEntity2.setComment(comments2);
    rdfTestEntity1.setUuidField(UUID.randomUUID());
    rdfTestEntity2.setUuidField(UUID.randomUUID());

    // set XML datatype fields in the first test RDF entity
    rdfTestEntity1.setByteField((byte) 5);
    rdfTestEntity1.setIntField(6);
    rdfTestEntity1.setLongField(7L);
    rdfTestEntity1.setFloatField(1.1F);
    rdfTestEntity1.setDoubleField(1.2D);
    rdfTestEntity1.setBigIntegerField(new BigInteger("100"));
    rdfTestEntity1.setBigDecimalField(new BigDecimal("100.001"));
    rdfTestEntity1.setCalendarField(Calendar.getInstance());
    rdfTestEntity1.setDateField(Calendar.getInstance().getTime());

    // set XML datatype fields in the linked test RDF entity
    rdfTestEntity2.setByteField((byte) 5);
    rdfTestEntity2.setIntField(6);
    rdfTestEntity2.setLongField(7L);
    rdfTestEntity2.setFloatField(1.1F);
    rdfTestEntity2.setDoubleField(1.2D);
    rdfTestEntity2.setBigIntegerField(new BigInteger("100"));
    rdfTestEntity2.setBigDecimalField(new BigDecimal("100.001"));
    rdfTestEntity2.setCalendarField(Calendar.getInstance());
    rdfTestEntity2.setDateField(Calendar.getInstance().getTime());

    // persist will cascade to rdfTestEntity2
    rdfEntityManager.persist(rdfTestEntity1);
    rdfTestEntityURIs[serialNbr - 1] = rdfTestEntity1.getId();
  }

  /** Verifies that the given RDF test entity's fields have been correctly loaded from the RDF store.
   *
   * @param rdfTestEntity the given RDF test entity
   */
  private void verifyRDFTestEntity(final RDFTestEntity rdfTestEntity) {
    assert rdfTestEntity != null : "rdfTestEntity must not be null";
    assert rdfTestEntity.getDontCareField() == null;
    assert rdfTestEntity.getFavoriteTestRDFEntityPeer() != null;
    assert rdfTestEntity.getMaxNbrOfScooterRiders() == 2;
    assert rdfTestEntity.getMyPeers().size() == 1;
    assert rdfTestEntity.getName().indexOf("TestDomainEntity") > -1 : " name: '" + rdfTestEntity.getName() + "'";
    assert rdfTestEntity.getNumberOfCrew() == 1;
    assert rdfTestEntity.getComment().length == 2;
    assert rdfTestEntity.getByteField() == (byte) 5;
    assert rdfTestEntity.getIntField() == 6;
    assert rdfTestEntity.getLongField() == 7L;
    assert rdfTestEntity.getFloatField() > 1.0F;
    assert rdfTestEntity.getDoubleField() > 1.1D;
    assert rdfTestEntity.getBigIntegerField().equals(new BigInteger("100"));
    assert rdfTestEntity.getBigDecimalField().equals(new BigDecimal("100.001"));
    assert rdfTestEntity.getCalendarField() != null;
    assert rdfTestEntity.getDateField() != null;
    assert rdfTestEntity.getUuidField() != null;
  }

  /** Executes this application.
   *
   * @param args the command line arguments (unused)
   */
  public static void main(final String[] args) {
    final PerformanceBenchmark performanceBenchmark = new PerformanceBenchmark();
    performanceBenchmark.initialization();
    performanceBenchmark.createLinkedRDFTestEntities();
    performanceBenchmark.queryInstanceURIs();
    performanceBenchmark.readLinkedRDFTestEntities();
    performanceBenchmark.finalization();
  }
}
