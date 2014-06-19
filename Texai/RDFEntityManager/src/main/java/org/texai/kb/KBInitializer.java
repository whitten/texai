/*
 * KBInitializer.java
 *
 * Created on Nov 21, 2010, 6:30:24 PM
 *
 * Description: Initializes the OpenCyc knowledge base.
 *
 * Copyright (C) Nov 21, 2010, Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.kb;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import net.jcip.annotations.NotThreadSafe;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFUtility;
import org.texai.turtleStatementParser.TurtleStatementParser;
import org.texai.turtleStatementParser.misc.ParsedTurtleStatementHandler;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**  Initializes a repository with turtle-formatted RDF statements, such as those created by RDFEntityManager.export(...).
 *
 * @author reed
 */
@NotThreadSafe
public final class KBInitializer implements ParsedTurtleStatementHandler {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(KBInitializer.class);
  /** the RDF entity manager */
  private final RDFEntityManager rdfEntityManager;
  /** the statement file path */
  private String statementFilePath = "../Main/data/kb-statements.txt";
  /** the repository name */
  private String repositoryName = Constants.OPEN_CYC;
  /** the statement count */
  private int statementCount = 0;
  /** the repository connection */
  private RepositoryConnection repositoryConnection;

  /** Constructs a new KBInitializer instance.
   * @param rdfEntityManager the RDF entity manager
   */
  public KBInitializer(final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    this.rdfEntityManager = rdfEntityManager;
  }

  /** Constructs a new KBInitializer instance.
   * @param repositoryConnection the repository connection
   */
  public KBInitializer(final RepositoryConnection repositoryConnection) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";

    this.repositoryConnection = repositoryConnection;
    rdfEntityManager = null;
  }

  /** Initializes the OpenCyc knowledge base by default unless this instance was constructed
   * with a given repository connection.
   */
  public void process() {
    LOGGER.info("Turtle-format RDF input file path: " + statementFilePath);
    if (rdfEntityManager != null) {
      LOGGER.info("repository name:                   " + repositoryName);
      repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    } else {
      LOGGER.info("repository name:                   " + repositoryConnection.getRepository().getDataDir());
    }
    // establish a transaction
    try {
      assert repositoryConnection.isAutoCommit();
      repositoryConnection.setAutoCommit(false);
      LOGGER.info("beginning repository size " + repositoryConnection.size());
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }

    // process the input file
    final BufferedInputStream inputStream;
    try {
      assert (new File(statementFilePath).exists()) : statementFilePath + " not found";
      inputStream = new BufferedInputStream(new FileInputStream(statementFilePath));
      final TurtleStatementParser turtleStatementParser = TurtleStatementParser.makeTurtleStatementParser(
              inputStream,
              this); // parsedTurtleStatementHandler
      turtleStatementParser.getStatements(); // see handleStatement method
      inputStream.close();
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }

    // commit the final transaction
    try {
      assert !repositoryConnection.isAutoCommit();
      repositoryConnection.commit();
      repositoryConnection.setAutoCommit(true);
      LOGGER.info("ending repository size    " + repositoryConnection.size());
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
    LOGGER.info(statementCount + " statements loaded");
  }

  /** Handles a parsed turtle statement.
   *
   * @param statement the statement
   */
  @Override
  public void handleStatement(final Statement statement) {
    LOGGER.debug("statement: " + RDFUtility.formatStatementAsTurtle(statement));
    try {
      repositoryConnection.add(statement);
      statementCount++;
      if (statementCount % 10000 == 0) {
        LOGGER.info(statementCount + " " + RDFUtility.formatStatement(statement));
      }
      if (statementCount % 5000 == 0) {
        // commit the transaction
        assert !repositoryConnection.isAutoCommit();
        repositoryConnection.commit();
      }
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Gets the statement file path.
   *
   * @return the statement file path
   */
  public String getStatementFilePath() {
    return statementFilePath;
  }

  /** Sets the statement file path.
   *
   * @param statementFilePath the statement file path
   */
  public void setStatementFilePath(final String statementFilePath) {
    //Preconditions
    assert statementFilePath != null : "statementFilePath must not be null";
    assert !statementFilePath.isEmpty() : "statementFilePath must not be empty";

    this.statementFilePath = statementFilePath;
  }

  /** Gets the repository name.
   *
   * @return the repositoryName
   */
  public String getRepositoryName() {
    return repositoryName;
  }

  /** Sets the repository name.
   *
   * @param repositoryName the repositoryName to set
   */
  public void setRepositoryName(final String repositoryName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(repositoryName);

    this.repositoryName = repositoryName;
  }

  /** Executes this application.
   *
   * @param args the command line arguments (unused)
   */
  public static void main(final String[] args) {
    CacheInitializer.initializeCaches();
    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    final KBInitializer kbInitializer = new KBInitializer(rdfEntityManager);
    kbInitializer.process();
    rdfEntityManager.close();
    DistributedRepositoryManager.shutDown();
    CacheManager.getInstance().shutdown();
  }
}
