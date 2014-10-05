package org.texai.tamperEvidentLogs;

import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.texai.kb.CacheInitializer;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;
import org.texai.kb.persistence.RDFEntityRemover;
import org.texai.x509.X509Utils;

/**
 * TELogBenchmark.java
 *
 * Description: Performs a benchmark for tamper-evident keyed log items.
 *
 * Copyright (C) Oct 4, 2014, Stephen L. Reed, Texai.org.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation,
 * Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
public class TELogBenchmark {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(TELogBenchmark.class);
  // the test log name
  private final static String TEST_LOG = "TestLog";

  /**
   * Creates a new instance of TELogBenchmark.
   */
  public TELogBenchmark() {
  }

  private void benchmark() {
    String name = TEST_LOG;
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);
    Logger.getLogger(RDFEntityRemover.class).setLevel(Level.WARN);
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addTestRepositoryPath(
            "TamperEvidentLogs",
            true); // isRepositoryDirectoryCleaned
    X509Utils.addBouncyCastleSecurityProvider();
    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    TELogAccess instance = new TELogAccess(rdfEntityManager);

    LOGGER.info("benchmark");
    instance.createTELogHeader(TEST_LOG);
    final int nbrIterations = 1000;
    final long beginMillis = System.currentTimeMillis();
    for (int i = 1; i < nbrIterations; i++) {
      if (i % 50 == 0) {
        LOGGER.info(i);
      }
      instance.appendTELogItemEntry(name, i, "test chaos value");
    }
    final long durationMillis = System.currentTimeMillis() - beginMillis;

    LOGGER.info("duration milliseconds:  " + durationMillis);
    LOGGER.info("log entries per second: " + 1000.0 * (float) nbrIterations / (float) durationMillis);
    rdfEntityManager.close();
    DistributedRepositoryManager.shutDown();
    CacheManager.getInstance().shutdown();
    JournalWriter.deleteJournalFiles();
  }

  /**
   * Executes this application.
   *
   * @param args the command line arguments - unused.
   */
  public static void main(final String[] args) {
    final TELogBenchmark teLogBenchmark = new TELogBenchmark();
    teLogBenchmark.benchmark();
  }

}
