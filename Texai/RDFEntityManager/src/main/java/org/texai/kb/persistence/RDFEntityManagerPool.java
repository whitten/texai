/*
 * RDFEntityManagerPool.java
 *
 * Created on Jun 16, 2009, 6:44:44 PM
 *
 * Description: Provides a pool of RDF entity manager instances.
 *
 * Copyright (C) Jun 16, 2009 Stephen L. Reed.
 */

package org.texai.kb.persistence;


import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.StackObjectPool;

/** Given a PoolableObjectFactory, this class will maintain a simple pool of RDF entity manager instances. A finite number of "sleeping"
 * or idle instances is enforced, but when the pool is empty, new instances are created to support the new load.
 * Hence this class places no limit on the number of "active" instances created by the pool, but is quite useful
 * for re-using Objects without introducing artificial limits.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public final class RDFEntityManagerPool extends StackObjectPool {

  /** the RDF entity manager factory */
  private final PoolableObjectFactory rdfEntityManagerFactory;

  /**
   * Creates a new instance of RDFEntityManagerPool.
   *
   * @param rdfEntityManagerFactory the PoolableObjectFactory to use to create, validate and destroy objects
   * @param maxIdle cap on the number of "sleeping" instances in the pool
   */
  public RDFEntityManagerPool(final PoolableObjectFactory rdfEntityManagerFactory, final int maxIdle) {
    super(rdfEntityManagerFactory, maxIdle);

    this.rdfEntityManagerFactory = rdfEntityManagerFactory;
  }

  /** Gets the RDF entity manager factory.
   *
   * @return the RDF entity manager factory
   */
  public PoolableObjectFactory getRDFEntityLoaderFactory() {
    return rdfEntityManagerFactory;
  }
}
