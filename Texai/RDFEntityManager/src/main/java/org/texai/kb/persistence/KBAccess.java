/*
 * KBAccess.java
 *
 * Created on Jan 31, 2009, 3:27:29 PM
 *
 * Description: Provides knowledge base access methods.
 *
 * Copyright (C) Jan 31, 2009 Stephen L. Reed.
 */
package org.texai.kb.persistence;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.log4j.Logger;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.texai.kb.Constants;
import org.texai.kb.object.AbstractKBObject;
import org.texai.kb.object.ClassKBObject;
import org.texai.kb.object.ContextKBObject;
import org.texai.kb.object.IndividualKBObject;
import org.texai.kb.object.PropertyKBObject;
import org.texai.kb.restriction.domainEntity.AbstractRestriction;
import org.texai.kb.restriction.domainEntity.AllValuesFromRestriction;
import org.texai.kb.restriction.domainEntity.CardinalityRestriction;
import org.texai.kb.restriction.domainEntity.HasValueRestriction;
import org.texai.kb.restriction.domainEntity.MaxCardinalityRestriction;
import org.texai.kb.restriction.domainEntity.MinCardinalityRestriction;
import org.texai.kb.restriction.domainEntity.SomeValuesFromRestriction;
import org.texai.util.TexaiException;

/** Provides knowledge base access methods.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public final class KBAccess {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(KBAccess.class);
  /** the term http://sw.cyc.com/2006/07/27/cyc/UniversalVocabularyMt */
  private final URI uriUniversalVocabularyMt;
  /** the RDF entity manager */
  private final RDFEntityManager rdfEntityManager;
  /** the rdf:type URI string */
  public static final String TERM_RDF_TYPE = RDF.TYPE.toString();
  /** the KB objects cache */
  private final Cache kbObjectsCache = CacheManager.getInstance().getCache(Constants.CACHE_KB_OBJECTS);
  /** the property restrictions cache */
  private final Cache propertyRestrictionsCache = CacheManager.getInstance().getCache(Constants.CACHE_PROPERTY_RESTRICTIONS);
  /** the subject property restrictions cache */
  private final Cache subjectPropertyRestrictionsCache = CacheManager.getInstance().getCache(Constants.CACHE_SUBJECT_PROPERTY_RESTRICTIONS);

  /** Constructs a new KBAccess instance.
   *
   * @param rdfEntityManager the RDF entity manager
   */
  public KBAccess(final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "kbEntityManager must not be null";

    uriUniversalVocabularyMt = new URIImpl(Constants.TERM_UNIVERSAL_VOCABULARY_MT);
    this.rdfEntityManager = rdfEntityManager;

    //Postconditions
    assert kbObjectsCache != null : "cache not found for: " + Constants.CACHE_KB_OBJECTS;
    assert propertyRestrictionsCache != null : "cache not found for: " + Constants.CACHE_PROPERTY_RESTRICTIONS;
    assert subjectPropertyRestrictionsCache != null : "cache not found for: " + Constants.CACHE_SUBJECT_PROPERTY_RESTRICTIONS;
  }

  /** Returns the KB object having the given subject, as a wrapped statement set as contrasted with a semantically annotated domain entity.
   *
   * @param repositoryName the repository name
   * @param subject the subject URI
   * @return the KB object having the given subject, as a wrapped statement set, or null if no statements found
   */
  public AbstractKBObject findKBObject(
          final String repositoryName,
          final URI subject) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert subject != null : "subject must not be null";

    // look in the cache first for the KB object
    final String key = repositoryName + "," + subject;

    Element element = kbObjectsCache.get(key);
    if (element == null) {
      // not found in the cache, so query its constituent statements from the given repository
      final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
      final Set<Statement> statements = new HashSet<>();
      try {
        // iterate over repository statements having the given subject
        final RepositoryResult<Statement> repositoryResult = repositoryConnection.getStatements(subject, null, null, false);
        while (repositoryResult.hasNext()) {
          statements.add(repositoryResult.next());
        }
      } catch (final Exception ex) {
        throw new TexaiException(ex);
      }
      final AbstractKBObject kbObject;
      if (rdfEntityManager.isClassTerm(repositoryName, subject)) {
        kbObject = new ClassKBObject(statements, repositoryName);
      } else if (rdfEntityManager.isContextTerm(repositoryName, subject)) {
        kbObject = new ContextKBObject(statements, repositoryName);
      } else if (rdfEntityManager.isPropertyTerm(repositoryName, subject)) {
        kbObject = new PropertyKBObject(statements, repositoryName);
      } else if (rdfEntityManager.isIndividualTerm(repositoryName, subject)) {
        kbObject = new IndividualKBObject(statements, repositoryName);
      } else {
        assert false : "undefined term: " + subject;
        return null;
      }
      // save the KB object in the cache
      kbObjectsCache.put(new Element(key, kbObject));
      return kbObject;
    } else {
      return (AbstractKBObject) element.getValue();
    }
  }

  /** Persists the given KB object, which is a wrapped statement set as contrasted with a semantically annotated domain entity.
   *
   * @param repositoryName the repository name
   * @param kbObject the given KB object
   */
  public void persistKBObject(
          final String repositoryName,
          final AbstractKBObject kbObject) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert kbObject != null : "kbObject must not be null";

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    try {
      for (final Statement statement : kbObject.getStatements()) {
        repositoryConnection.add(statement);
      }
    } catch (RepositoryException ex) {
      throw new TexaiException(ex);
    }
    final String key = repositoryName + "," + kbObject.getSubject();
    kbObjectsCache.put(new Element(key, kbObject));
  }

  /** Adds the given type relationship.
   *
   * @param repositoryName the repository name
   * @param instance the instance term
   * @param type the type term
   */
  public void addType(
          final String repositoryName,
          final URI instance,
          final URI type) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert instance != null : "instance must not be null";
    assert type != null : "type must not be null";
    assert rdfEntityManager.isClassTerm(repositoryName, type) : "type must be a class term";

    rdfEntityManager.add(repositoryName, new StatementImpl(instance, RDF.TYPE, type));
  }

  /** Removes the given type relationship.
   *
   * @param repositoryName the repository name
   * @param instance the instance term
   * @param type the type term
   */
  public void removeType(
          final String repositoryName,
          final URI instance,
          final URI type) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert instance != null : "instance must not be null";
    assert type != null : "type must not be null";

    rdfEntityManager.remove(repositoryName, new StatementImpl(instance, RDF.TYPE, type));
  }

  /** Adds the given superClassOf relationship.
   *
   * @param repositoryName the repository name
   * @param subClass the subclass term
   * @param superClass the superclass term
   */
  public void addSuperClassOf(
          final String repositoryName,
          final URI subClass,
          final URI superClass) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert subClass != null : "subClass must not be null";
    assert superClass != null : "superClass must not be null";
    assert rdfEntityManager.isClassTerm(repositoryName, subClass) : "subClass "
            + RDFUtility.formatResource(subClass) + " must be a class term";
    assert rdfEntityManager.isClassTerm(repositoryName, superClass) : "superClass "
            + RDFUtility.formatResource(superClass) + " must be a class term";

    rdfEntityManager.add(repositoryName, new StatementImpl(subClass, RDFS.SUBCLASSOF, superClass));
  }

  /** Removes the given superClassOf relationship.
   *
   * @param repositoryName the repository name
   * @param subClass the subclass term
   * @param superClass the superclass term
   */
  public void removeSuperClassOf(
          final String repositoryName,
          final URI subClass,
          final URI superClass) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert subClass != null : "subClass must not be null";
    assert superClass != null : "superClass must not be null";

    rdfEntityManager.remove(repositoryName, new StatementImpl(subClass, RDFS.SUBCLASSOF, superClass));
  }

  /** Adds the given property restriction as a super class of the given subject class.
   *
   * @param repositoryName the repository name
   * @param restriction the property restriction
   * @param subject the subject class
   */
  public void addRestriction(
          final String repositoryName,
          final AbstractRestriction restriction,
          final URI subject) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert restriction != null : "restriction must not be null";
    assert subject != null : "subject must not be null";
    assert rdfEntityManager.isClassTerm(repositoryName, subject);

    final Set<AbstractRestriction> existingRestrictions = getRestrictions(
            repositoryName,
            subject,
            restriction.getOnProperty());
    if (existingRestrictions.contains(restriction)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("skipping existing restriction " + restriction);
      }
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("adding restriction " + restriction);
      }
      rdfEntityManager.persist(restriction, repositoryName);
      addType(
              repositoryName,
              restriction.getId(),
              OWL.CLASS);
      addSuperClassOf(
              repositoryName,
              subject,
              restriction.getId());

      String key = repositoryName + "," + RDFUtility.formatResource(restriction.getOnProperty());
      Element element = propertyRestrictionsCache.get(key);
      if (element == null) {
        final Set<AbstractRestriction> restrictions = new HashSet<>();
        restrictions.add(restriction);
        propertyRestrictionsCache.put(new Element(key, restrictions));
      } else {
        @SuppressWarnings("unchecked")
        final Set<AbstractRestriction> restrictions = (Set<AbstractRestriction>) element.getValue();
        restrictions.add(restriction);
        propertyRestrictionsCache.put(element);
      }

      key = repositoryName + "," + RDFUtility.formatResource(subject) + "," + RDFUtility.formatResource(restriction.getOnProperty());
      element = subjectPropertyRestrictionsCache.get(key);
      if (element == null) {
        final Set<AbstractRestriction> restrictions = new HashSet<>();
        restrictions.add(restriction);
        subjectPropertyRestrictionsCache.put(new Element(key, restrictions));
      } else {
        @SuppressWarnings("unchecked")
        final Set<AbstractRestriction> restrictions = (Set<AbstractRestriction>) element.getValue();
        restrictions.add(restriction);
        subjectPropertyRestrictionsCache.put(element);
      }
    }
  }

  /** Removes the given property restriction as a super class of the given subject class, and removes the restriction
   * from the repository if it is no longer referenced.
   *
   * @param repositoryName the repository name
   * @param restriction the property restriction
   * @param subject the subject class
   * @return whether the given property restriction indeed was removed
   */
  public boolean removeRestriction(
          final String repositoryName,
          final AbstractRestriction restriction,
          final URI subject) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert restriction != null : "restriction must not be null";
    assert subject != null : "subject must not be null";
    assert rdfEntityManager.isClassTerm(repositoryName, subject);

    boolean isRemoved = false;
    final Set<AbstractRestriction> existingRestrictions = getRestrictions(
            repositoryName,
            subject,
            restriction.getOnProperty());
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("existing restrictions " + existingRestrictions);
    }
    if (existingRestrictions.contains(restriction)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("removing existing restriction relationship " + restriction);
      }
      isRemoved = true;
      removeType(
              repositoryName,
              restriction.getId(),
              OWL.CLASS);
      removeSuperClassOf(
              repositoryName,
              subject,
              restriction.getId());
      removeRestrictionFromCaches(repositoryName, subject, restriction);
      if (getRestrictions(
              repositoryName,
              subject,
              restriction.getOnProperty()).isEmpty()) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("removing unreferenced restriction object " + restriction);
        }
        rdfEntityManager.remove(restriction, repositoryName);
      }
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("restriction to be removed does not exist on given subject " + restriction);
        removeRestrictionFromCaches(repositoryName, subject, restriction);
      }
    }
    return isRemoved;
  }

  /** Removes the given restriction from the caches.
   *
   * @param repositoryName the repository name
   * @param subject the subject class
   * @param restriction the restriction
   */
  private void removeRestrictionFromCaches(
          final String repositoryName,
          final URI subject,
          final AbstractRestriction restriction) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert restriction != null : "restriction must not be null";
    assert subject != null : "subject must not be null";

    String key = repositoryName + "," + RDFUtility.formatResource(restriction.getOnProperty());
    Element element = propertyRestrictionsCache.get(key);
    if (element != null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("removing restriction from cache, key: " + key + ", cache: " + Constants.CACHE_PROPERTY_RESTRICTIONS);
      }
      @SuppressWarnings("unchecked")
      final Set<AbstractRestriction> restrictions = (Set<AbstractRestriction>) element.getValue();
      restrictions.remove(restriction);
      propertyRestrictionsCache.put(element);
    }

    key = repositoryName + "," + RDFUtility.formatResource(subject) + "," + RDFUtility.formatResource(restriction.getOnProperty());
    element = subjectPropertyRestrictionsCache.get(key);
    if (element != null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("removing restriction from cache, key: " + key + ", cache: " + Constants.CACHE_SUBJECT_PROPERTY_RESTRICTIONS);
      }
      @SuppressWarnings("unchecked")
      final Set<AbstractRestriction> restrictions = (Set<AbstractRestriction>) element.getValue();
      restrictions.remove(restriction);
      subjectPropertyRestrictionsCache.put(element);
    }
  }

  /** Adds the given all-values-from property restriction as a super class of the given subject class.
   *
   * @param repositoryName the repository name
   * @param subject the subject class
   * @param onProperty the property for which this restriction applies
   * @param allValuesClass the class that constrains all of the values of the associated property
   */
  public void addAllValuesFromRestriction(
          final String repositoryName,
          final URI subject,
          final URI onProperty,
          final URI allValuesClass) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert subject != null : "subject must not be null";
    assert rdfEntityManager.isClassTerm(repositoryName, subject);
    assert onProperty != null : "onProperty must not be null";
    assert rdfEntityManager.isPropertyTerm(repositoryName, onProperty);
    assert allValuesClass != null : "allValuesClass must not be null";
    assert rdfEntityManager.isClassTerm(repositoryName, allValuesClass);

    addRestriction(
            repositoryName,
            new AllValuesFromRestriction(onProperty, allValuesClass),
            subject);
  }

  /** Adds the given some-values-from property restriction as a super class of the given subject class.
   *
   * @param repositoryName the repository name
   * @param subject the subject class
   * @param onProperty the property for which this restriction applies
   * @param someValuesClass the class that constrains all of the values of the associated property
   */
  public void addSomeValuesFromRestriction(
          final String repositoryName,
          final URI subject,
          final URI onProperty,
          final URI someValuesClass) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert subject != null : "subject must not be null";
    assert rdfEntityManager.isClassTerm(repositoryName, subject);
    assert onProperty != null : "onProperty must not be null";
    assert rdfEntityManager.isPropertyTerm(repositoryName, onProperty);
    assert someValuesClass != null : "someValuesClass must not be null";
    assert rdfEntityManager.isClassTerm(repositoryName, someValuesClass);

    addRestriction(
            repositoryName,
            new SomeValuesFromRestriction(onProperty, someValuesClass),
            subject);
  }

  /** Adds the given has-value property restriction as a super class of the given subject class.
   *
   * @param repositoryName the repository name
   * @param subject the subject class
   * @param onProperty the property for which this restriction applies
   * @param hasValueClass the class that constrains some of the values of the associated property
   */
  public void addHasValueFromRestriction(
          final String repositoryName,
          final URI subject,
          final URI onProperty,
          final URI hasValueClass) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert subject != null : "subject must not be null";
    assert rdfEntityManager.isClassTerm(repositoryName, subject);
    assert onProperty != null : "onProperty must not be null";
    assert rdfEntityManager.isPropertyTerm(repositoryName, onProperty);
    assert hasValueClass != null : "hasValueClass must not be null";
    assert rdfEntityManager.isClassTerm(repositoryName, hasValueClass);

    addRestriction(
            repositoryName,
            new HasValueRestriction(onProperty, hasValueClass),
            subject);
  }

  /** Adds the given cardinality property restriction as a super class of the given subject class.
   *
   * @param repositoryName the repository name
   * @param subject the subject class
   * @param onProperty the property for which this restriction applies
   * @param cardinality the cardinality of the values of the associated property
   */
  public void addCardinalityRestriction(
          final String repositoryName,
          final URI subject,
          final URI onProperty,
          final long cardinality) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert subject != null : "subject must not be null";
    assert rdfEntityManager.isClassTerm(repositoryName, subject);
    assert onProperty != null : "onProperty must not be null";
    assert rdfEntityManager.isPropertyTerm(repositoryName, onProperty);
    assert cardinality >= 0 : "cardinality must not be negative";

    addRestriction(
            repositoryName,
            new CardinalityRestriction(onProperty, cardinality),
            subject);
  }

  /** Adds the given minimum cardinality property restriction as a super class of the given subject class.
   *
   * @param repositoryName the repository name
   * @param subject the subject class
   * @param onProperty the property for which this restriction applies
   * @param minCardinality the minimum cardinality of the values of the associated property
   */
  public void addMinCardinalityRestriction(
          final String repositoryName,
          final URI subject,
          final URI onProperty,
          final long minCardinality) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert subject != null : "subject must not be null";
    assert rdfEntityManager.isClassTerm(repositoryName, subject);
    assert onProperty != null : "onProperty must not be null";
    assert rdfEntityManager.isPropertyTerm(repositoryName, onProperty);
    assert minCardinality >= 0 : "minCardinality must not be negative";

    addRestriction(
            repositoryName,
            new MinCardinalityRestriction(onProperty, minCardinality),
            subject);
  }

  /** Adds the given maximum cardinality property restriction as a super class of the given subject class.
   *
   * @param repositoryName the repository name
   * @param subject the subject class
   * @param onProperty the property for which this restriction applies
   * @param maxCardinality the maximum cardinality of the values of the associated property
   */
  public void addMaxCardinalityRestriction(
          final String repositoryName,
          final URI subject,
          final URI onProperty,
          final long maxCardinality) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert subject != null : "subject must not be null";
    assert rdfEntityManager.isClassTerm(repositoryName, subject);
    assert onProperty != null : "onProperty must not be null";
    assert rdfEntityManager.isPropertyTerm(repositoryName, onProperty);
    assert maxCardinality >= 0 : "maxCardinality must not be negative";

    addRestriction(
            repositoryName,
            new MaxCardinalityRestriction(onProperty, maxCardinality),
            subject);
  }

  /** Gets all the applicable property restrictions for the given predicate.
   *
   * @param repositoryName the repository name
   * @param predicate the predicate (property)
   * @return all the applicable property restrictions for the given predicate
   */
  @SuppressWarnings("unchecked")
  public Set<AbstractRestriction> getRestrictionsByPredicate(
          final String repositoryName,
          final URI predicate) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert predicate != null : "predicate must not be null";

    final String key = repositoryName + "," + RDFUtility.formatResource(predicate);
    Element element = propertyRestrictionsCache.get(key);
    if (element == null) {
      final Set<AbstractRestriction> restrictions = new HashSet<>();
      restrictions.addAll(rdfEntityManager.find(
              new URIImpl(Constants.OWL_NAMESPACE + "onProperty"),
              predicate,
              AllValuesFromRestriction.class,
              repositoryName));
      restrictions.addAll(rdfEntityManager.find(
              new URIImpl(Constants.OWL_NAMESPACE + "onProperty"),
              predicate,
              CardinalityRestriction.class,
              repositoryName));
      restrictions.addAll(rdfEntityManager.find(
              new URIImpl(Constants.OWL_NAMESPACE + "onProperty"),
              predicate,
              HasValueRestriction.class,
              repositoryName));
      restrictions.addAll(rdfEntityManager.find(
              new URIImpl(Constants.OWL_NAMESPACE + "onProperty"),
              predicate,
              MaxCardinalityRestriction.class,
              repositoryName));
      restrictions.addAll(rdfEntityManager.find(
              new URIImpl(Constants.OWL_NAMESPACE + "onProperty"),
              predicate,
              MinCardinalityRestriction.class,
              repositoryName));
      restrictions.addAll(rdfEntityManager.find(
              new URIImpl(Constants.OWL_NAMESPACE + "onProperty"),
              predicate,
              SomeValuesFromRestriction.class,
              repositoryName));
      propertyRestrictionsCache.put(new Element(key, restrictions));
      return restrictions;
    } else {
      return (Set<AbstractRestriction>) element.getValue();
    }
  }

  /** Gets all the applicable property restrictions for the given subject and predicate.
   *
   * @param repositoryName the repository name
   * @param subject the subject term, which can be a class or individual
   * @param predicate the predicate (property)
   * @return all the applicable property restrictions for the given subject and predicate
   */
  @SuppressWarnings("unchecked")
  public Set<AbstractRestriction> getRestrictions(
          final String repositoryName,
          final URI subject,
          final URI predicate) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert subject != null : "subject must not be null";
    assert predicate != null : "predicate must not be null";

    final String key = repositoryName + "," + RDFUtility.formatResource(subject) + "," + RDFUtility.formatResource(predicate);
    Element element = subjectPropertyRestrictionsCache.get(key);
    if (element == null) {
      final Set<AbstractRestriction> restrictions = new HashSet<>();
      final Set<URI> classes = new HashSet<>();
      if (rdfEntityManager.isClassTerm(repositoryName, subject)) {
        classes.addAll(rdfEntityManager.getSubClassOfQueries().getSuperClasses(repositoryName, subject));
        classes.add(subject);
      } else {
        assert rdfEntityManager.isIndividualTerm(repositoryName, subject);
        final Set<URI> types = rdfEntityManager.getTypeQueries().getDirectTypes(repositoryName, subject);
        classes.addAll(types);
        for (final URI type : types) {
          classes.addAll(rdfEntityManager.getSubClassOfQueries().getSuperClasses(repositoryName, type));
        }
      }
      final Set<AbstractRestriction> candidateRestrictions = getRestrictionsByPredicate(repositoryName, predicate);
      for (final AbstractRestriction candidateRestriction : candidateRestrictions) {
        if (classes.contains(candidateRestriction.getId())) {
          restrictions.add(candidateRestriction);
        }
      }
      subjectPropertyRestrictionsCache.put(new Element(key, restrictions));
      return restrictions;
    } else {
      return (Set<AbstractRestriction>) element.getValue();
    }
  }

  /** Returns whether the given term is the subject of any statement.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @return whether the given term is the subject of any statement
   */
  public boolean doesTermExist(final String repositoryName, final URI term) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";

    final String key = repositoryName + "," + term;
    if (kbObjectsCache.isKeyInCache(key)) {
      return true;
    }

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    try {
      final BooleanQuery booleanQuery = repositoryConnection.prepareBooleanQuery(
              QueryLanguage.SPARQL,
              "ASK  { ?s ?p  ?o }");
      booleanQuery.setBinding("s", term);
      return booleanQuery.evaluate();
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Removes all statements having the given term as a subject or as an object
   *
   * @param repositoryName the repository name
   * @param term the given term
   */
  public void removeTerm(final String repositoryName, final URI term) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    try {
      LOGGER.info("removing statements having subject " + term);
      final TupleQuery subjectTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SPARQL,
              "SELECT ?p ?o WHERE { ?s ?p  ?o }");
      subjectTupleQuery.setBinding("s", term);
      TupleQueryResult tupleQueryResult = subjectTupleQuery.evaluate();
      while (tupleQueryResult.hasNext()) {
        final BindingSet bindingSet = tupleQueryResult.next();
        final URI predicate = (URI) bindingSet.getBinding("p").getValue();
        final Value object = bindingSet.getBinding("o").getValue();
        final Statement statement = new StatementImpl(term, predicate, object);
        LOGGER.debug("  removing: " + RDFUtility.formatStatementAsTurtle(statement));
        rdfEntityManager.removeStatement(repositoryConnection, statement);
      }
      LOGGER.info("removing statements having object " + term);
      final TupleQuery objectTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SPARQL,
              "SELECT ?s ?o WHERE { ?s ?p  ?o }");
      objectTupleQuery.setBinding("o", term);
      tupleQueryResult = objectTupleQuery.evaluate();
      while (tupleQueryResult.hasNext()) {
        final BindingSet bindingSet = tupleQueryResult.next();
        final Resource subject = (Resource) bindingSet.getBinding("o").getValue();
        final URI predicate = (URI) bindingSet.getBinding("p").getValue();
        final Statement statement = new StatementImpl(subject, predicate, term);
        LOGGER.debug("  removing: " + RDFUtility.formatStatementAsTurtle(statement));
        rdfEntityManager.removeStatement(repositoryConnection, statement);
      }
    } catch (final RepositoryException | MalformedQueryException | QueryEvaluationException ex) {
      throw new TexaiException(ex);
    }
    final String key = repositoryName + "," + term;
    kbObjectsCache.remove(key);
  }

  /** Asserts the defining statements for a new RDF class.  This method is useful in the case where a new RDF class is required
   * but no direct instances of the RDF class will ever be instantiated as Java objects.
   *
   * @param repositoryName the repository name
   * @param classURI the RDF class URI
   * @param comment the RDF class comment
   * @param typeURIs the RDF class typeURIs
   * @param subClassOfURIs the RDF classes for which this RDF class is a subclass
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  public void defineRDFClass(
          final String repositoryName,
          final URI classURI,
          final String comment,
          final List<URI> typeURIs,
          final List<URI> subClassOfURIs,
          final BufferedWriter writer) {
    //preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert classURI != null : "classURI must not be null";
    assert comment != null : "comment must not be null";
    assert !comment.isEmpty() : "comment must not be empty";
    assert typeURIs != null : "typeURIs must not be null";
    assert !typeURIs.isEmpty() : "typeURIs must not be empty";            // NOPMD
    assert subClassOfURIs != null : "subClassOfURIs must not be null";
    assert !subClassOfURIs.isEmpty() : "typeURIs must not be empty";

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    Statement statement = new StatementImpl(
            classURI,
            RDFS.COMMENT,
            new LiteralImpl(comment));
    if (writer == null) {
      rdfEntityManager.addStatement(repositoryConnection, statement, uriUniversalVocabularyMt);
    } else {
      try {
        writer.write(RDFUtility.formatStatementAsTurtle(statement));
        writer.newLine();
      } catch (IOException ex) {
        throw new TexaiException(ex);
      }
    }
    LOGGER.info("added: " + RDFUtility.formatStatement(statement));            // NOPMD
    for (final URI typeURI : typeURIs) {
      statement = new StatementImpl(
              classURI,
              RDF.TYPE,
              typeURI);

      if (writer == null) {
        rdfEntityManager.addStatement(repositoryConnection, statement, uriUniversalVocabularyMt);
      } else {
        try {
          writer.write(RDFUtility.formatStatementAsTurtle(statement));
          writer.newLine();
        } catch (IOException ex) {
          throw new TexaiException(ex);
        }
      }
      LOGGER.info("added: " + RDFUtility.formatStatement(statement));
    }
    for (final URI subClassOfURI : subClassOfURIs) {
      statement = new StatementImpl(
              classURI,
              RDFS.SUBCLASSOF,
              subClassOfURI);
      if (writer == null) {
        rdfEntityManager.addStatement(repositoryConnection, statement, uriUniversalVocabularyMt);
      } else {
        try {
          writer.write(RDFUtility.formatStatementAsTurtle(statement));
          writer.newLine();
        } catch (IOException ex) {
          throw new TexaiException(ex);
        }
      }
      LOGGER.info("added: " + RDFUtility.formatStatement(statement));
    }
  }

  /** Asserts the defining statements for a new RDF individual.  This method is useful in the case where a new RDF individual is required
   * but no corresponding Java class has been created.
   *
   * @param repositoryName the repository name
   * @param individualURI the individual URI
   * @param comment the RDF individual comment
   * @param typeURIs the RDF class typeURIs
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  public void defineRDFIndividual(
          final String repositoryName,
          final URI individualURI,
          final String comment,
          final List<URI> typeURIs,
          final BufferedWriter writer) {
    //preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert individualURI != null : "classURI must not be null";
    assert comment != null : "comment must not be null";
    assert !comment.isEmpty() : "comment must not be empty";
    assert typeURIs != null : "typeURIs must not be null";
    assert !typeURIs.isEmpty() : "typeURIs must not be empty";            // NOPMD

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    Statement statement = new StatementImpl(
            individualURI,
            RDFS.COMMENT,
            new LiteralImpl(comment));
    if (writer == null) {
      rdfEntityManager.addStatement(repositoryConnection, statement, uriUniversalVocabularyMt);
    } else {
      try {
        writer.write(RDFUtility.formatStatementAsTurtle(statement));
        writer.newLine();
      } catch (IOException ex) {
        throw new TexaiException(ex);
      }
    }
    LOGGER.info("added: " + RDFUtility.formatStatement(statement));            // NOPMD
    for (final URI typeURI : typeURIs) {
      statement = new StatementImpl(
              individualURI,
              RDF.TYPE,
              typeURI);
      if (writer == null) {
        rdfEntityManager.addStatement(repositoryConnection, statement, uriUniversalVocabularyMt);
      } else {
        try {
          writer.write(RDFUtility.formatStatementAsTurtle(statement));
          writer.newLine();
        } catch (IOException ex) {
          throw new TexaiException(ex);
        }
      }
      LOGGER.info("added: " + RDFUtility.formatStatement(statement));
    }
  }

  /** Asserts the defining statements for a new RDF context.
   *
   * @param repositoryName the repository name
   * @param contextURI the RDF context URI
   * @param comment the RDF class comment
   * @param typeURIs the RDF context typeURIs
   * @param genlMtURIs the RDF contexts for which this RDF context is a specialization
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  public void defineRDFContext(
          final String repositoryName,
          final URI contextURI,
          final String comment,
          final List<URI> typeURIs,
          final List<URI> genlMtURIs,
          final BufferedWriter writer) {
    //preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert contextURI != null : "contextURI must not be null";
    assert comment != null : "comment must not be null";
    assert !comment.isEmpty() : "comment must not be empty";
    assert typeURIs != null : "typeURIs must not be null";
    assert genlMtURIs != null : "genlMtURIs must not be null";

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    Statement statement = new StatementImpl(
            contextURI,
            RDFS.COMMENT,
            new LiteralImpl(comment));
    if (writer == null) {
      rdfEntityManager.addStatement(repositoryConnection, statement, uriUniversalVocabularyMt);
    } else {
      try {
        writer.write(RDFUtility.formatStatementAsTurtle(statement));
        writer.newLine();
      } catch (IOException ex) {
        throw new TexaiException(ex);
      }
    }
    LOGGER.info("added: " + RDFUtility.formatStatement(statement));            // NOPMD
    if (typeURIs.isEmpty()) {
      typeURIs.add(new URIImpl(Constants.TERM_MICROTHEORY));
    }
    for (final URI typeURI : typeURIs) {
      statement = new StatementImpl(
              contextURI,
              RDF.TYPE,
              typeURI);
      if (writer == null) {
        rdfEntityManager.addStatement(repositoryConnection, statement, uriUniversalVocabularyMt);
      } else {
        try {
          writer.write(RDFUtility.formatStatementAsTurtle(statement));
          writer.newLine();
        } catch (IOException ex) {
          throw new TexaiException(ex);
        }
      }
      LOGGER.info("added: " + RDFUtility.formatStatement(statement));
    }
    if (typeURIs.isEmpty()) {
      typeURIs.add(new URIImpl(Constants.TERM_BASE_KB));
    }
    for (final URI subClassOfURI : genlMtURIs) {
      statement = new StatementImpl(
              contextURI,
              RDFS.SUBCLASSOF,
              subClassOfURI);
      if (writer == null) {
        rdfEntityManager.addStatement(repositoryConnection, statement, uriUniversalVocabularyMt);
      } else {
        try {
          writer.write(RDFUtility.formatStatementAsTurtle(statement));
          writer.newLine();
        } catch (IOException ex) {
          throw new TexaiException(ex);
        }
      }
      LOGGER.info("added: " + RDFUtility.formatStatement(statement));
    }
  }

  /** Asserts that the two given classes are disjoint.
   *
   * @param repositoryName the repository name
   * @param classURI1 the first given class
   * @param classURI2 the second given class
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  public void assertDisjoint(
          final String repositoryName,
          final URI classURI1,
          final URI classURI2,
          final BufferedWriter writer) {
    //preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert classURI1 != null : "classURI1 must not be null";
    assert classURI2 != null : "classURI2 must not be null";

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    Statement statement = new StatementImpl(
            classURI1,
            OWL.DISJOINTWITH,
            classURI2);
    if (writer == null) {
      rdfEntityManager.addStatement(repositoryConnection, statement, uriUniversalVocabularyMt);
    } else {
      try {
        writer.write(RDFUtility.formatStatementAsTurtle(statement));
        writer.newLine();
      } catch (IOException ex) {
        throw new TexaiException(ex);
      }
    }
    LOGGER.info("added: " + RDFUtility.formatStatement(statement));
    statement = new StatementImpl(
            classURI2,
            OWL.DISJOINTWITH,
            classURI1);
    if (writer == null) {
      rdfEntityManager.addStatement(repositoryConnection, statement, uriUniversalVocabularyMt);
    } else {
      try {
        writer.write(RDFUtility.formatStatementAsTurtle(statement));
        writer.newLine();
      } catch (IOException ex) {
        throw new TexaiException(ex);
      }
    }
    LOGGER.info("added: " + RDFUtility.formatStatement(statement));
  }

  /**
   * Asserts the defining statements for a new RDF predicate.
   *
   * @param repositoryName the repository name
   * @param predicateURI the RDF predicate
   * @param comment the RDF predicate comment
   * @param typeURIs the RDF predicate typeURIs
   * @param subPropertyOfs the RDF predicates for which this RDF predicate is a subproperty
   * @param domainURI the type of the RDF subject that can be used with this predicate
   * @param rangeURI the type of the RDF object that can be used with this predicate
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  public void defineRDFPredicate(
          final String repositoryName,
          final URI predicateURI,
          final String comment,
          final List<URI> typeURIs,
          final List<URI> subPropertyOfs,
          final URI domainURI,
          final URI rangeURI,
          final BufferedWriter writer) {
    //preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert predicateURI != null : "predicateURI must not be null";
    assert typeURIs != null : "typeURIs must not be null";
    assert !typeURIs.isEmpty() : "typeURIs must not be empty";
    assert subPropertyOfs != null : "subPropertyOfs must not be null";
    assert !subPropertyOfs.isEmpty() : "typeURIs must not be empty";
    assert domainURI != null : "domainURI must not be null";
    assert rangeURI != null : "rangeURI must not be null";

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    Statement statement;
    if (comment != null && !comment.isEmpty()) {
      statement = new StatementImpl(
              predicateURI,
              RDFS.COMMENT,
              new LiteralImpl(comment));
      if (writer == null) {
        rdfEntityManager.addStatement(repositoryConnection, statement, uriUniversalVocabularyMt);
      } else {
        try {
          writer.write(RDFUtility.formatStatementAsTurtle(statement));
          writer.newLine();
        } catch (IOException ex) {
          throw new TexaiException(ex);
        }
      }
      LOGGER.info("added: " + RDFUtility.formatStatement(statement));
    }
    for (final URI type : typeURIs) {
      statement = new StatementImpl(
              predicateURI,
              RDF.TYPE,
              type);
      if (writer == null) {
        rdfEntityManager.addStatement(repositoryConnection, statement, uriUniversalVocabularyMt);
      } else {
        try {
          writer.write(RDFUtility.formatStatementAsTurtle(statement));
          writer.newLine();
        } catch (IOException ex) {
          throw new TexaiException(ex);
        }
      }
      LOGGER.info("added: " + RDFUtility.formatStatement(statement));
    }
    for (final URI subPropertyOf : subPropertyOfs) {
      statement = new StatementImpl(
              predicateURI,
              RDFS.SUBPROPERTYOF,
              subPropertyOf);
      if (writer == null) {
        rdfEntityManager.addStatement(repositoryConnection, statement, uriUniversalVocabularyMt);
      } else {
        try {
          writer.write(RDFUtility.formatStatementAsTurtle(statement));
          writer.newLine();
        } catch (IOException ex) {
          throw new TexaiException(ex);
        }
      }
      LOGGER.info("added: " + RDFUtility.formatStatement(statement));
    }
    statement = new StatementImpl(
            predicateURI,
            RDFS.DOMAIN,
            domainURI);
    if (writer == null) {
      rdfEntityManager.addStatement(repositoryConnection, statement, uriUniversalVocabularyMt);
    } else {
      try {
        writer.write(RDFUtility.formatStatementAsTurtle(statement));
        writer.newLine();
      } catch (IOException ex) {
        throw new TexaiException(ex);
      }
    }
    LOGGER.info("added: " + RDFUtility.formatStatement(statement));
    statement = new StatementImpl(
            predicateURI,
            RDFS.RANGE,
            rangeURI);
    if (writer == null) {
      rdfEntityManager.addStatement(repositoryConnection, statement, uriUniversalVocabularyMt);
    } else {
      try {
        writer.write(RDFUtility.formatStatementAsTurtle(statement));
        writer.newLine();
      } catch (IOException ex) {
        throw new TexaiException(ex);
      }
    }
    LOGGER.info("added: " + RDFUtility.formatStatement(statement));
  }

  /** Renames the given URI.
   *
   * @param repositoryName the repository name
   * @param oldURI the old URI
   * @param newURI the new URI
   */
  @SuppressWarnings("deprecation")
  public void renameURI(
          final String repositoryName,
          final URI oldURI,
          final URI newURI) {
    //preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert oldURI != null : "oldURI must not be null";
    assert newURI != null : "newURI must not be null";

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    final List<Statement> oldStatements = new ArrayList<>();
    try {
      @SuppressWarnings("deprecation")
      final boolean isExternalTransaction = !repositoryConnection.isAutoCommit();
      if (!isExternalTransaction) {
        // establish a transaction
        assert repositoryConnection.isAutoCommit();
        repositoryConnection.setAutoCommit(false);
      }
      LOGGER.info("gathering statements having subject: " + oldURI);
      RepositoryResult<Statement> repositoryResult = repositoryConnection.getStatements(oldURI, null, null, true);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("replacing subjects " + oldURI + " --> " + newURI);
      }
      while (repositoryResult.hasNext()) {
        oldStatements.add(repositoryResult.next());
      }
      repositoryResult.close();
      int statementCount = 0;
      for (final Statement oldStatement : oldStatements) {
        Statement newStatement;
        if (oldStatement.getContext() == null) {
          newStatement = new StatementImpl(
                  newURI,
                  oldStatement.getPredicate(),
                  oldStatement.getObject());
        } else {
          newStatement = new ContextStatementImpl(
                  newURI,
                  oldStatement.getPredicate(),
                  oldStatement.getObject(),
                  oldStatement.getContext());
        }
        rdfEntityManager.removeStatement(repositoryConnection, oldStatement);
        assert !repositoryConnection.hasStatement(oldStatement, false) : "old statement not removed";
        LOGGER.info("removed:   " + RDFUtility.formatStatement(oldStatement));
        rdfEntityManager.addStatement(repositoryConnection, newStatement);
        LOGGER.info("added: " + RDFUtility.formatStatement(newStatement));
        statementCount = statementCount + 2;
        if (statementCount > 1000 && !isExternalTransaction) {
          statementCount = 0;
          // commit transaction
          repositoryConnection.commit();
        }
      }

      statementCount = 0;
      LOGGER.info("gathering statements having predicate: " + oldURI);
      repositoryResult = repositoryConnection.getStatements(null, oldURI, null, true);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("replacing predicates " + oldURI + " --> " + newURI);
      }
      oldStatements.clear();
      while (repositoryResult.hasNext()) {
        oldStatements.add(repositoryResult.next());
      }
      repositoryResult.close();
      for (final Statement oldStatement : oldStatements) {
        Statement newStatement;
        if (oldStatement.getContext() == null) {
          newStatement = new StatementImpl(
                  oldStatement.getSubject(),
                  newURI,
                  oldStatement.getObject());
        } else {
          newStatement = new ContextStatementImpl(
                  oldStatement.getSubject(),
                  newURI,
                  oldStatement.getObject(),
                  oldStatement.getContext());
        }
        rdfEntityManager.removeStatement(repositoryConnection, oldStatement);
        LOGGER.info("removed:   " + RDFUtility.formatStatement(oldStatement));
        rdfEntityManager.addStatement(repositoryConnection, newStatement);
        LOGGER.info("added: " + RDFUtility.formatStatement(newStatement));
        statementCount = statementCount + 2;
        if (statementCount > 1000 && !isExternalTransaction) {
          statementCount = 0;
          // commit transaction
          repositoryConnection.commit();
        }
      }

      statementCount = 0;
      LOGGER.info("gathering statements having object: " + oldURI);
      repositoryResult = repositoryConnection.getStatements(null, null, oldURI, true);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("replacing objects " + oldURI + " --> " + newURI);
      }
      oldStatements.clear();
      while (repositoryResult.hasNext()) {
        oldStatements.add(repositoryResult.next());
      }
      repositoryResult.close();
      for (final Statement oldStatement : oldStatements) {
        Statement newStatement;
        if (oldStatement.getContext() == null) {
          newStatement = new StatementImpl(
                  oldStatement.getSubject(),
                  oldStatement.getPredicate(),
                  newURI);
        } else {
          newStatement = new ContextStatementImpl(
                  oldStatement.getSubject(),
                  oldStatement.getPredicate(),
                  newURI,
                  oldStatement.getContext());
        }
        rdfEntityManager.removeStatement(repositoryConnection, oldStatement);
        LOGGER.info("removed:   " + RDFUtility.formatStatement(oldStatement));
        rdfEntityManager.addStatement(repositoryConnection, newStatement);
        LOGGER.info("added: " + RDFUtility.formatStatement(newStatement));
        statementCount = statementCount + 2;
        if (statementCount > 1000 && !isExternalTransaction) {
          statementCount = 0;
          // commit transaction
          repositoryConnection.commit();
        }
      }

      statementCount = 0;
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("replacing contexts " + oldURI + " --> " + newURI);
      }
      repositoryResult = repositoryConnection.getStatements(null, null, null, false, oldURI);
      oldStatements.clear();
      while (repositoryResult.hasNext()) {
        oldStatements.add(repositoryResult.next());
      }
      repositoryResult.close();
      for (final Statement oldStatement : oldStatements) {
        final Statement newStatement = new ContextStatementImpl(
                oldStatement.getSubject(),
                oldStatement.getPredicate(),
                oldStatement.getObject(),
                newURI);
        rdfEntityManager.removeStatement(repositoryConnection, oldStatement);
        LOGGER.info("removed:   " + RDFUtility.formatStatement(oldStatement));
        rdfEntityManager.addStatement(repositoryConnection, newStatement);
        LOGGER.info("added: " + RDFUtility.formatStatement(newStatement));
        statementCount = statementCount + 2;
        if (statementCount > 1000 && !isExternalTransaction) {
          statementCount = 0;
          // commit transaction
          repositoryConnection.commit();
        }
      }

      if (!isExternalTransaction) {
        // commit final transaction
        assert !repositoryConnection.isAutoCommit();
        repositoryConnection.commit();
        repositoryConnection.setAutoCommit(true);
      }
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
  }
}
