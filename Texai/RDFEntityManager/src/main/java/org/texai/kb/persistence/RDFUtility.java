/*
 * RDFUtility.java
 *
 * Created on August 7, 2007, 10:24 AM
 *
 * Description: Provides RDF store utilities.
 *
 * Copyright (C) August 7, 2007 Stephen L. Reed.
 */
package org.texai.kb.persistence;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import javax.xml.bind.DatatypeConverter;
import net.jcip.annotations.NotThreadSafe;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.LazyLoader;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.texai.kb.Constants;
import org.texai.kb.persistence.lazy.LazyList;
import org.texai.kb.persistence.lazy.LazySet;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/** Provides RDF store utilities.
 *
 * @author reed
 */
@NotThreadSafe
public final class RDFUtility {

  /** the Sesame value factory */
  private final ValueFactory valueFactory = new ValueFactoryImpl();
  /** the knowledge base access object */
  private final KBAccess kbAccess;

  /** Creates a new instance of RDFUtility.
   *
   * @param rdfEntityManager the entity manager
   */
  public RDFUtility(final RDFEntityManager rdfEntityManager) {
    //preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    kbAccess = new KBAccess(rdfEntityManager);
  }

  public static class ResourceComparator implements Comparator<Resource> {

    /** Constructs a new ResourceComparator instance. */
    public ResourceComparator() {
    }

    /** Compares the two given URIs.
     *
     * @param resource1 the first given resource
     * @param resource2 the second given resource
     * @return -1 if resource1 is less than resource2, 0 if they are equal, otherwise return +1
     */
    @Override
    public int compare(final Resource resource1, final Resource resource2) {
      //preconditions
      assert resource1 != null : "resource1 must not be null";
      assert resource2 != null : "resource2 must not be null";

      return resource1.toString().compareTo(resource2.toString());
    }
  }

  /** Returns whether the given object is a URI that represents a logical variable.
   *
   * @param value the given object
   * @return whether the given object is a URI that represents a logical variable
   */
  public static boolean isVariableURI(final Value value) {
    return value instanceof URI && ((URI) value).getLocalName().startsWith("?");
  }

  /** Returns whether the given object is a URI that represents an instance term, i.e. the name ends in a digit.
   *
   * @param value the given object
   * @return whether the given object is a URI that represents an instance term, i.e. the name ends in a digit
   */
  public static boolean isInstanceURI(final Value value) {
    if (value instanceof URI) {
      final String termName = ((URI) value).getLocalName();
      return Character.isUpperCase(termName.charAt(0))
              && Character.isDigit(termName.charAt(termName.length() - 1));
    } else {
      return false;
    }
  }

  /** Gets a default property URI for the persistent field.
   *
   * @param className the name of the persistent class containing the field
   * @param fieldName the field name
   * @param fieldType the field type
   * @return a default property URI
   */
  public static URI getDefaultPropertyURI(
          final String className,
          final String fieldName,
          final Class<?> fieldType) {
    //Preconditions
    assert fieldName != null : "fieldName must not be null";
    assert !fieldName.isEmpty() : "fieldName must not be empty";

    // changed to longer, more storage-inefficient, full class name in order to eliminate namespace collisions
    //final String className = StringUtils.getLowerCasePredicateName(field.getDeclaringClass().getSimpleName());
    String uriString = Constants.TEXAI_NAMESPACE + className + "_" + StringUtils.capitalize(fieldName);
    if (List.class.isAssignableFrom(fieldType) || fieldType.isArray()) {
      // list or array
      uriString = uriString + "_list";
    } else if (Set.class.isAssignableFrom(fieldType)) {
      // set
      if (uriString.endsWith("s")) {
        uriString = uriString.substring(0, uriString.length() - 1);
      }
    }
    return new URIImpl(uriString);
  }

  /** Returns the class name as embedded in the id that is generated by default for a persistent object.
   *
   * @param id the persistent object id
   * @return the class name
   */
  public static String getDefaultClassFromId(final URI id) {
    //Preconditions
    assert id != null : "id must not be null";

    return getDefaultClassFromIdString(id.toString());
  }

  /** Returns the class name as embedded in the id that is generated by default for a persistent object.
   *
   * @param idString the persistent object URI as a string
   * @return the class name
   */
  public static String getDefaultClassFromIdString(final String idString) {
    //Preconditions
    assert idString != null : "idString must not be null";
    assert !idString.isEmpty() : "idString must not be empty";

    int index = idString.lastIndexOf('/');
    if (index == -1) {
      return "";
    }
    final String localName = idString.substring(index + 1);
    index = localName.indexOf('_');
    if (index > -1) {
      return localName.substring(0, index);
    } else {
      return "";
    }
  }

  /** Sorts the given collection of statements.
   *
   * @param statements the given collection of statements
   * @return the sorted list of statements
   */
  public static List<Statement> sortStatements(final Collection<Statement> statements) {
    //preconditions
    assert statements != null : "statements must not be null";

    final List<Statement> sortedStatements = new ArrayList<>(statements);
    final StatementComparator statementComparator = new StatementComparator();
    Collections.sort(sortedStatements, statementComparator);
    return sortedStatements;
  }

  /** Provides a statement comparator. */
  public static class StatementComparator implements Comparator<Statement> {

    /** Constructs a new StatementComparator instance. */
    public StatementComparator() {
    }

    /** Compares the two given statements.
     *
     * @param statement1 the first given statement
     * @param statement2 the second given statement
     * @return -1 if statement1 is less than statement2, 0 if they are equal, otherwise return +1
     */
    @Override
    public int compare(final Statement statement1, final Statement statement2) {
      //preconditions
      assert statement1 != null : "statement1 must not be null";
      assert statement2 != null : "statement2 must not be null";

      return statement1.toString().compareTo(statement2.toString());
    }
  }

  /** Provides a turtle statement comparator. */
  public static class TurtleStatementComparator implements Comparator<Statement> {

    /** Constructs a new StatementComparator instance. */
    public TurtleStatementComparator() {
    }

    /** Compares the two given statements.
     *
     * @param statement1 the first given statement
     * @param statement2 the second given statement
     * @return -1 if statement1 is less than statement2, 0 if they are equal, otherwise return +1
     */
    @Override
    public int compare(final Statement statement1, final Statement statement2) {
      //preconditions
      assert statement1 != null : "statement1 must not be null";
      assert statement2 != null : "statement2 must not be null";

      return formatStatementAsTurtle(statement1).compareTo(formatStatementAsTurtle(statement2));
    }
  }

  /** Formats the given statements.
   *
   * @param statements the statements
   * @return the formatted statement in which namespaces are represented by prefixes
   */
  public static String formatStatements(final Collection<Statement> statements) {
    //preconditions
    assert statements != null : "statements must not be null";

    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("{");
    boolean isFirst = true;
    final Iterator<Statement> statements_iter = statements.iterator();
    while (statements_iter.hasNext()) {
      if (isFirst) {
        isFirst = false;
      } else {
        stringBuilder.append(", ");
      }
      stringBuilder.append(formatStatement(statements_iter.next()));
    }
    stringBuilder.append("}");
    return stringBuilder.toString();
  }

  /** Formats an RDF statement.
   *
   * @param statement the statement
   * @return the formatted statement in which namespaces are represented by prefixes
   */
  public static String formatStatement(final Statement statement) {
    //preconditions
    assert statement != null : "statement must not be null";

    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("(");
    if (statement.getSubject() instanceof URI) {
      stringBuilder.append(formatResource((URI) statement.getSubject()));
    } else {
      stringBuilder.append(statement.getSubject().toString());
    }
    stringBuilder.append(" ");
    stringBuilder.append(formatResource(statement.getPredicate()));
    stringBuilder.append(" ");
    if (statement.getObject() instanceof URI) {
      stringBuilder.append(formatResource((URI) statement.getObject()));
    } else {
      stringBuilder.append(statement.getObject().toString());
    }
    if (statement.getContext() != null) {
      stringBuilder.append(" [");
      stringBuilder.append(formatResource((URI) statement.getContext()));
      stringBuilder.append("]");
    }
    stringBuilder.append(")");
    return stringBuilder.toString();
  }

  /** Formats an RDF statement as Turtle, extended from the specification to support context.
   * See http://www.w3.org/TeamSubmission/turtle/ .
   *
   * @param statement the statement
   * @return the formatted statement in which namespaces are represented by prefixes
   */
  public static String formatStatementAsTurtle(final Statement statement) {
    //preconditions
    assert statement != null : "statement must not be null";

    final StringBuilder stringBuilder = new StringBuilder();
    if (statement.getSubject() instanceof URI) {
      stringBuilder.append(formatURIAsTurtle((URI) statement.getSubject()));
    } else {
      stringBuilder.append(statement.getSubject().toString());
    }
    stringBuilder.append(" ");
    stringBuilder.append(formatURIAsTurtle(statement.getPredicate()));

    stringBuilder.append(" ");
    if (statement.getObject() instanceof URI) {
      stringBuilder.append(formatURIAsTurtle((URI) statement.getObject()));
    } else {
      stringBuilder.append(statement.getObject().toString());
    }
    if (statement.getContext() != null) {
      stringBuilder.append(" in ");
      stringBuilder.append(formatURIAsTurtle((URI) statement.getContext()));
    }
    stringBuilder.append(" .");
    return stringBuilder.toString();
  }

  /** Formats an RDF statement as XML Turtle.
   *
   * @param statement the statement
   * @return the formatted statement in which namespaces are represented by prefixes
   */
  public static String formatStatementAsXMLTurtle(final Statement statement) {
    //preconditions
    assert statement != null : "statement must not be null";

    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("<turtle><![CDATA[");
    stringBuilder.append(formatStatementAsTurtle(statement));
    stringBuilder.append("]]></turtle>");
    return stringBuilder.toString();
  }

  /** Formats the given resources with common namespaces.
   *
   * @param resources the given URI or BNode
   * @return the formatted URI
   */
  public static String formatResources(final Collection<? extends Resource> resources) {
    //preconditions
    assert resources != null : "resources must not be null";

    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('{');
    boolean isFirst = true;
    for (final Resource resource : resources) {
      if (isFirst) {
        isFirst = false;
      } else {
        stringBuilder.append(", ");
      }
      stringBuilder.append(formatResource(resource));
    }
    stringBuilder.append('}');
    return stringBuilder.toString();
  }

  /** Formats the given resources with common namespaces after sortng them.
   *
   * @param resources the given URI or BNode
   * @return the formatted URI
   */
  public static String formatSortedResources(final Collection<? extends Resource> resources) {
    //preconditions
    assert resources != null : "resources must not be null";

    final List<Resource> sortedResources = new ArrayList<>(resources);
    Collections.sort(sortedResources, new ResourceComparator());
    return formatResources(sortedResources);
  }

  /** Formats the given resource with common namespaces.
   *
   * @param resource the given URI or BNode
   * @return the formatted URI
   */
  public static String formatResource(final Resource resource) {
    //preconditions
    assert resource != null : "resource must not be null";

    final String formattedResource;
    if (resource instanceof URI) {
      final URI uri = (URI) resource;
      final String namespace = uri.getNamespace();
      switch (namespace) {
        case Constants.XSD_NAMESPACE:
          formattedResource = "xsd:" + uri.getLocalName();
          break;
        case Constants.RDF_NAMESPACE:
          formattedResource = "rdf:" + uri.getLocalName();
          break;
        case Constants.RDFS_NAMESPACE:
          formattedResource = "rdfs:" + uri.getLocalName();
          break;
        case Constants.OWL_NAMESPACE:
          formattedResource = "owl:" + uri.getLocalName();
          break;
        case Constants.CYC_NAMESPACE:
          formattedResource = "cyc:" + uri.getLocalName();
          break;
        case Constants.TEXAI_NAMESPACE:
          formattedResource = "texai:" + uri.getLocalName();
          break;
        default:
          formattedResource = uri.toString();
          break;
      }
    } else {
      formattedResource = resource.toString();
    }

    //Postconditions
    assert formattedResource != null : "formattedResource must not be null";
    assert !formattedResource.isEmpty() : "formattedResource must not be empty";

    return formattedResource;
  }

  /** Formats the given URI with common namespaces.
   *
   * @param value the given URI or BNode
   * @return the formatted URI
   */
  public static String formatValue(final Value value) {
    //preconditions
    assert value != null : "value must not be null";

    final String formattedValue;
    if (value instanceof Resource) {
      formattedValue = formatResource((Resource) value);
    } else {
      formattedValue = value.toString();
    }

    //Postconditions
    assert formattedValue != null : "formattedValue must not be null";
    assert !formattedValue.isEmpty() : "formattedValue must not be empty";

    return formattedValue;
  }

  /** Formats the given URI with common namespaces.   See http://www.w3.org/TeamSubmission/turtle/ .
   *
   * @param uri the given URI
   * @return the formatted URI
   */
  public static String formatURIAsTurtle(final URI uri) {
    //preconditions
    assert uri != null : "uri must not be null";

    final StringBuilder stringBuilder = new StringBuilder();
    final String namespace = uri.getNamespace();
    switch (namespace) {
      case Constants.XSD_NAMESPACE:
        stringBuilder.append("xsd:");
        stringBuilder.append(uri.getLocalName());
        break;
      case Constants.RDF_NAMESPACE:
        stringBuilder.append("rdf:");
        stringBuilder.append(uri.getLocalName());
        break;
      case Constants.RDFS_NAMESPACE:
        stringBuilder.append("rdfs:");
        stringBuilder.append(uri.getLocalName());
        break;
      case Constants.OWL_NAMESPACE:
        stringBuilder.append("owl:");
        stringBuilder.append(uri.getLocalName());
        break;
      case Constants.CYC_NAMESPACE:
        stringBuilder.append("cyc:");
        stringBuilder.append(uri.getLocalName());
        break;
      case Constants.TEXAI_NAMESPACE:
        if (uri.getLocalName().startsWith("?")) {
          stringBuilder.append(uri.getLocalName());
        } else {
          stringBuilder.append("texai:");
          stringBuilder.append(uri.getLocalName());
        }
        break;
      default:
        stringBuilder.append(uri.toString());
        break;
    }

    return stringBuilder.toString();
  }

  /** Returns the namespace for the given namespace alias.
   *
   * @param namespaceAlias the given namespace alias
   * @return the namespace for the given namespace alias, or null if not found
   */
  public static String decodeNamespace(final String namespaceAlias) {
    //preconditions
    assert namespaceAlias != null : "namespaceAlias must not be null";
    assert !namespaceAlias.isEmpty() : "namespaceAlias must not be empty";
    switch (namespaceAlias) {
      case "rdf":
        return Constants.RDF_NAMESPACE;
      case "rdfs":
        return Constants.RDFS_NAMESPACE;
      case "owl":
        return Constants.OWL_NAMESPACE;
      case "cyc":
        return Constants.CYC_NAMESPACE;
      case "texai":
        return Constants.TEXAI_NAMESPACE;
      default:
        return null;
    }
  }

  /** Returns a URI from the given string that may include an alias prefix.
   *
   * @param uriString the given string
   * @return a URI
   */
  public static URI makeURIFromAlias(final String uriString) {
    //preconditions
    assert uriString != null : "uriString must not be null";
    assert !uriString.isEmpty() : "uriString must not be empty";

    final int index = uriString.indexOf(':');
    if (index == -1 || uriString.endsWith(":")) {
      throw new TexaiException("invalid URI string " + uriString);
    }
    final String namespaceAlias = uriString.substring(0, index);
    final String namespace = decodeNamespace(namespaceAlias);
    if (namespace == null) {
      return new URIImpl(uriString);
    } else {
      return new URIImpl(namespace + uriString.substring(index + 1));
    }
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

    kbAccess.defineRDFClass(
            repositoryName,
            classURI, comment,
            typeURIs,
            subClassOfURIs,
            writer);
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

    kbAccess.defineRDFIndividual(
            repositoryName,
            individualURI,
            comment,
            typeURIs,
            writer);
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

    kbAccess.defineRDFContext(
            repositoryName,
            contextURI,
            comment,
            typeURIs,
            genlMtURIs,
            writer);
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

    kbAccess.assertDisjoint(
            repositoryName,
            classURI1,
            classURI2,
            writer);
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

    kbAccess.defineRDFPredicate(
            repositoryName,
            predicateURI,
            comment,
            typeURIs,
            subPropertyOfs,
            domainURI,
            rangeURI,
            writer);
  }

  /** Renames the given URI.
   *
   * @param repositoryName the repository name
   * @param oldURI the old URI
   * @param newURI the new URI
   */
  public void renameURI(
          final String repositoryName,
          final URI oldURI,
          final URI newURI) {
    //preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert oldURI != null : "oldURI must not be null";
    assert newURI != null : "newURI must not be null";

    kbAccess.renameURI(
            repositoryName,
            oldURI,
            newURI);
  }

  /** Returns the lazy loaded object represented by the given proxy and as a side effect loads the object
   * into its instance field.  If the given object is not a proxy then it is returned directly. Ordinarily,
   * proxy objects are automatically loaded whenever any of their methods, except toString(), are called.
   * However there may be situations in which they are assigned to another field.  Unless they are initialized
   * they will not be persisted.
   *
   * @param proxyObject the given proxy object
   * @return the lazy loaded object represented by the given proxy and as a side effect loads the object
   * into its instance field.  If the given object is not a proxy then it is returned directly.
   */
  public Object initializeProxyObject(final Object proxyObject) {
    //preconditions
    assert proxyObject != null : "proxyObject must not be null";

    if (proxyObject instanceof LazySet) {
      // load the set and return it
      return ((LazySet) proxyObject).getLoadedSet();
    } else if (proxyObject instanceof LazyList) {
      // load the list and return it
      return ((LazyList) proxyObject).getLoadedList();
    } else if (proxyObject instanceof Factory) {
      final LazyLoader rdfEntityLazyLoader = (LazyLoader) ((Factory) proxyObject).getCallback(0);
      try {
        // load the proxied object and return it
        return rdfEntityLazyLoader.loadObject();
      } catch (final Exception ex) {
        throw new TexaiException(ex);
      }
    } else {
      // not a proxy
      return proxyObject;
    }
  }

  /** Returns a literal value corresponding to the given date.
   *
   * @param date the given date
   * @return a literal value corresponding to the given date
   */
  public Literal getLiteralForDate(final Date date) {
    //preconditions
    assert date != null : "date must not be null";

    final Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.setTimeZone(TimeZone.getTimeZone("America/Chicago"));
    return getLiteralForCalendar(calendar);
  }

  /** Returns a literal value corresponding to the given calendar object.
   *
   * @param calendar the calendar object
   * @return a literal value corresponding to the given calendar object
   */
  public Literal getLiteralForCalendar(final Calendar calendar) {
    //preconditions
    assert calendar != null : "calendar must not be null";

    return valueFactory.createLiteral(DatatypeConverter.printDateTime(calendar), XMLSchema.DATETIME);
  }

  /** Gets the set of values, given the subject, property and context.
   *
   * @param subject the statment subject
   * @param predicate the predicate, i.e. the RDF property
   * @param context the context
   * @param repositoryConnection the repository connection
   * @return the set of object values
   */
  public static Set<Value> getObjectsGivenSubjectAndPredicate(
          final Resource subject,
          final Resource predicate,
          final Resource context,
          final RepositoryConnection repositoryConnection) {
    //preconditions
    assert subject != null : "subject must not be null";
    assert predicate != null : "predicate must not be null";
    assert repositoryConnection != null : "repositoryConnection must not be null";

    final Set<Value> values = new HashSet<>();
    try {
      // get the persisted map entry key RDF value
      final TupleQuery objectsTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              "SELECT o, c FROM CONTEXT c {s} p {o}");
      objectsTupleQuery.setBinding("s", subject);
      objectsTupleQuery.setBinding("p", predicate);
      objectsTupleQuery.setBinding("c", context);
      final TupleQueryResult tupleQueryResult = objectsTupleQuery.evaluate();
      while (tupleQueryResult.hasNext()) {
        values.add(tupleQueryResult.next().getBinding("o").getValue());
      }
      tupleQueryResult.close();

    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
    return values;
  }

  /** Gets the single value, given the subject, property and context.
   *
   * @param subject the statement subject
   * @param predicate the predicate, i.e. the RDF property
   * @param context the context
   * @param repositoryConnection the repository connection
   * @return the single value, or null if not found
   */
  public static Value getObjectGivenSubjectAndPredicate(
          final Resource subject,
          final Resource predicate,
          final Resource context,
          final RepositoryConnection repositoryConnection) {
    //preconditions
    assert subject != null : "subject must not be null";
    assert predicate != null : "predicate must not be null";
    assert repositoryConnection != null : "repositoryConnection must not be null";

    final Value value;
    try {
      // get the persisted map entry key RDF value
      final TupleQuery objectsTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              "SELECT o, c FROM CONTEXT c {s} p {o}");
      objectsTupleQuery.setBinding("s", subject);
      objectsTupleQuery.setBinding("p", predicate);
      objectsTupleQuery.setBinding("c", context);
      final TupleQueryResult tupleQueryResult = objectsTupleQuery.evaluate();
      if (tupleQueryResult.hasNext()) {
        value = tupleQueryResult.next().getBinding("o").getValue();
      } else {
        value = null;
      }
      tupleQueryResult.close();

    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
    return value;
  }

  /** Returns whether the given string has an invalid character that would prevent loading the RDF statement.
   *
   * @param string the given string
   * @return
   */
  public static boolean hasInvalidCharacter(final String string) {
    //Preconditions
    assert string != null : "string must not be null";

    for (final char ch : string.toCharArray()) {
      if (ch == '\u211d'
              || ch == '\ufb05'
              || ch == '\ufb06') {
        return true;
      }
    }
    return false;
  }

}