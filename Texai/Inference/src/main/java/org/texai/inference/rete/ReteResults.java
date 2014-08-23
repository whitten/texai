/*
 * ReteResults.java
 *
 * Created on Aug 16, 2010, 3:09:45 PM
 *
 * Description: Provides a container for the Rete algorithm query results.
 *
 * Copyright (C) Aug 16, 2010, Stephen L. Reed.
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
package org.texai.inference.rete;

import java.util.Map;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.Value;

/** Provides a container for the Rete algorithm query results.
 *
 * @author reed
 */
@NotThreadSafe
public class ReteResults {

  /** the dictionary of satisfied queries and their respective bindings, satisfied query name --> binding dictionary, variable --> value */
  final Map<String, Map<String, Value>> resultsDictionary;

  /** Constructs a new ReteResults instance. */
  public ReteResults(final Map<String, Map<String, Value>> resultsDictionary) {
    //Preconditions
    assert resultsDictionary != null : "resultsDictionary must not be null";

    this.resultsDictionary = resultsDictionary;
  }

  /** Returns whether the query having the given name is satisfied.
   * 
   * @param queryContainerName the query container name
   * @return whether the query having the given name is satisfied
   */
  public boolean isQuerySatisfied(final String queryContainerName) {
    //Preconditions
    assert queryContainerName != null : "queryContainerName must not be null";
    assert !queryContainerName.isEmpty() : "queryContainerName must not be empty";

    return resultsDictionary.containsKey(queryContainerName);
  }

  /** Gets the binding dictionary for the given query name, or null if the query is not satisfied.
   *
   * @param queryContainerName the query container name
   * @return the binding dictionary for the given query name, or null if the query is not satisfied
   */
  public Map<String, Value> getBindingDictionary(final String queryContainerName) {
    //Preconditions
    assert queryContainerName != null : "queryContainerName must not be null";
    assert !queryContainerName.isEmpty() : "queryContainerName must not be empty";

    return resultsDictionary.get(queryContainerName);
  }

  /** Gets the bound value of the given variable in the given satisfied query, or null if not found.
   * 
   * @param queryContainerName the name of the satisfied query
   * @param variableName the variable name in the query select clause
   * @return the bound value of the given variable in the given satisfied query, or null if not found
   */
  public Value getBinding(final String queryContainerName, final String variableName) {
    //Preconditions
    assert queryContainerName != null : "queryContainerName must not be null";
    assert !queryContainerName.isEmpty() : "queryContainerName must not be empty";
    assert variableName != null : "variableName must not be null";
    assert !variableName.isEmpty() : "variableName must not be empty";

    final Map<String, Value> bindingDictionary = resultsDictionary.get(queryContainerName);
    if (bindingDictionary == null) {
      return null;
    }
    return bindingDictionary.get(variableName);
  }

  /** Returns the set of satisfied query names.
   *
   * @return the set of satisfied query names
   */
  public Set<String> getNamesOfSatisfiedQueries() {
    return resultsDictionary.keySet();
  }

  /** Returns the number of satisfied queries.
   * 
   * @return the number of satisfied queries
   */
  public int size() {
    return resultsDictionary.size();
  }

  /** Returns a string representation of this object.
   * 
   * @return  a string representation of this object
   */
  @Override
  public String toString() {
    return resultsDictionary.toString();
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object
   * @return whether some other object equals this one
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ReteResults other = (ReteResults) obj;
    if (this.resultsDictionary != other.resultsDictionary && (this.resultsDictionary == null || !this.resultsDictionary.equals(other.resultsDictionary))) {
      return false;
    }
    return true;
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    int hash = 5;
    hash = 19 * hash + (this.resultsDictionary != null ? this.resultsDictionary.hashCode() : 0);
    return hash;
  }
}
