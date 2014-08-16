/*
 * RDFRepositoryPartitionInfo.java
 *
 * Created on Dec 20, 2012, 3:18:19 PM
 *
 * Description: Provides a container for repository partition information.
 *
 * Copyright (C) Dec 20, 2012, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.kb.persistence;

import net.jcip.annotations.NotThreadSafe;
import org.openrdf.repository.RepositoryConnection;
import org.texai.util.StringUtils;

/** Provides a container for repository partition information.
 *
 * @author reed
 */
@NotThreadSafe
public class RDFRepositoryPartitionInfo {

  /** the partition number */
  private final int partitionNbr;
  /** the repository name */
  private final String repositoryName;
  /** the repository connection */
  private final RepositoryConnection repositoryConnection;
  /** the number of repository operations since the last commit */
  private int operationCnt = 0;

  /** Constructs a new RDFRepositoryPartitionInfo instance.
   *
   * @param partitionNbr the partition number
   * @param repositoryName the repository name
   * @param repositoryConnection the repository connection
   */
  public RDFRepositoryPartitionInfo(
          final int partitionNbr,
          final String repositoryName,
          final RepositoryConnection repositoryConnection) {
    assert partitionNbr > 0 : "partitionNbr must be positive";
    assert StringUtils.isNonEmptyString(repositoryName) : "repositoryName must be non-empty";
    assert repositoryConnection != null : "repositoryConnection must not be null";

    this.partitionNbr = partitionNbr;
    this.repositoryName = repositoryName;
    this.repositoryConnection = repositoryConnection;
  }

  /** Gets the partition number.
   *
   * @return the partitionNbr
   */
  public int getPartitionNbr() {
    return partitionNbr;
  }

  /** Gets the repository name.
   *
   * @return the repositoryName
   */
  public String getRepositoryName() {
    return repositoryName;
  }

  /** Gets the repository connection.
   *
   * @return the repositoryConnection
   */
  public RepositoryConnection getRepositoryConnection() {
    return repositoryConnection;
  }

  /** Gets the number of repository operations since the last commit.
   *
   * @return the operationCnt
   */
  public int getOperationCnt() {
    return operationCnt;
  }

  /** Sets the number of repository operations since the last commit.
   *
   * @param operationCnt the operationCnt to set
   */
  public void setOperationCnt(int operationCnt) {
    this.operationCnt = operationCnt;
  }
}
