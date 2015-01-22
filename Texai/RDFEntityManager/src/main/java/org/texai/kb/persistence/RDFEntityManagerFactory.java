/*
 * RDFEntityManagerFactory.java
 *
 * Created on Jun 16, 2009, 7:04:42 PM
 *
 * Description: Provides a factory to create RDF entity manager instances to populate the pool.
 *
 * Copyright (C) Jun 16, 2009 Stephen L. Reed.
 */

package org.texai.kb.persistence;


import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.log4j.Logger;

/** Provides a factory to create RDF entity manager instances to populate the pool.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public final class RDFEntityManagerFactory extends BasePoolableObjectFactory {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(RDFEntityManagerFactory.class);
  /** the indicator whether debug logging is enabled */
  private static final boolean IS_DEBUG_LOGGING_ENABLED = LOGGER.isDebugEnabled();

  /** Constructs a new RDFEntityManagerFactory instance. */
  public RDFEntityManagerFactory() {

  }
  /** Makes a new object for the pool.
   *
   * @return a new RDF entity manager instance
   */
  @Override
  public Object makeObject() {
    if (IS_DEBUG_LOGGING_ENABLED) {
      LOGGER.debug("made a new RDFEntityManager instance for the pool");
    }
    return new RDFEntityManager();
  }

  /** Destroys the given pooled object.
   *
   * @param obj the given object to destroy
   */
  @Override
  public void destroyObject(final Object obj) {
    //Preconditions
    assert obj != null : "obj must not be null";
    assert obj instanceof RDFEntityManager : "obj must be a RDFEntityManager";

    if (IS_DEBUG_LOGGING_ENABLED) {
      LOGGER.debug("destroyed a RDFEntityManager instance from the pool");
    }
    ((RDFEntityManager) obj).close();
  }
}
