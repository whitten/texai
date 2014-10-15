/*
 * AlbusHCSAccess.java
 *
 * Created on Mar 15, 2010, 2:32:43 PM
 *
 * Description: Provides access methods to Albus hierarchical control system domain objects.
 *
 * Copyright (C) Mar 15, 2010 reed.
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
package org.texai.ahcsSupport;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.util.TexaiException;

/** Provides access methods to Albus hierarchical control system domain objects.
 *
 * @author reed
 */
@NotThreadSafe
public class AlbusHCSAccess {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(AlbusHCSAccess.class);
  // the RDF entity manager
  private final RDFEntityManager rdfEntityManager;


  /** Constructs a new AlbusHCSAccess instance.
   *
   * @param rdfEntityManager the RDF entity manager
   */
  public AlbusHCSAccess(final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    this.rdfEntityManager = rdfEntityManager;
  }

  /** Gets the Albus hierarchical control system granularity level that is associated with the given role type.
   *
   * @param type the role type
   * @return the Albus hierarchical control system granularity level, or null if not found
   */
  public URI getAlbusHCSGranularityLevel(final URI type) {
    //Preconditions
    assert type != null : "type must not be null";

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository("NODES");
    try {
      final TupleQuery objectTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SPARQL,
              "SELECT ?o WHERE { ?s ?p  ?o }");
      objectTupleQuery.setBinding("s", type);
      objectTupleQuery.setBinding("p", AHCSConstants.ALBUS_HCS_GRANULARITY_LEVEL);
      TupleQueryResult tupleQueryResult = objectTupleQuery.evaluate();
      if (tupleQueryResult.hasNext()) {
        final BindingSet bindingSet = tupleQueryResult.next();
        final Value object = bindingSet.getBinding("o").getValue();
        assert !tupleQueryResult.hasNext();
        assert object instanceof URI;
        return (URI) object;
      } else {
        return null;
      }
    } catch (final RepositoryException | MalformedQueryException | QueryEvaluationException ex) {
      throw new TexaiException(ex);
    }
  }
}
