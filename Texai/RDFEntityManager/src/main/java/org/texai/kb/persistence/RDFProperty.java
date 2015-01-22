/*
 * RDFProperty.java
 *
 * Created on August 3, 2007, 2:32 PM
 *
 * Description: Indicates that the annotated field is an RDF predicate.  If the predicate term
 * is not currently defined in the RDF store, it will be created.
 *
 * Copyright (C) August 3, 2007 Stephen L. Reed.
 */

package org.texai.kb.persistence;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import javax.persistence.FetchType;

/** This interface defines the @RDFProperty annotation and its properties.
 *
 * @author reed
 */
@Target(FIELD)
@Documented
@Retention(RUNTIME)
public @interface RDFProperty {

  /** (Optional) Defines the RDF namespace prefix for predicates derived from the name of the annotated field. When present,
   * it takes precedence over the @RDFEntity namespace annotation.
   *
   * @return a string RDF namespace prefix
   */
  String namespace() default "";

  /** (Required) Defines the RDF predicate that is mapped to this association.
   *
   * @return a string RDF predicate URI
   */
  String predicate() default "";

  /** (Optional) Defines the RDF predicates for which this predicate is a specialization.  Each value must specify an existing RDF predicate.
   *
   * @return a string array of RDF predicate URIs
   */
  String[] subPropertyOf() default { };

  /** (Optional) Defines the type of the subject for this predicate.  The value must specify an existing class in the RDF store.
   * The default value is the class of the domain object that contains the field, prefixed by the namespace.
   *
   * @return a string class URI
   */
  String domain() default "";

  /** (Optional) Defines the type of the objects for this predicate.  The value must specify an existing class in the RDF store.  Unqualified
   * values are automatically prefixed with the namespace. The default value of this property is the class of field's value prefixed by
   * the namespace, or the corresponding XML schema datatype when applicable.
   *
   * @return a string class URI
   */
  String range() default "";

  /** (Optional) Defines whether the predicate is an inverse predicate with respect to the annotated field, in which case the
   * field value is mapped to the domain of the predicate and the java object is mapped to the range of the predicate.
   *
   * @return whether the predicate is an inverse predicate with respect to the annotated field
   */
  boolean inverse() default false;

  /** (Optional) Defines whether the value of the field should be lazily loaded or whether it must be eagerly fetched.
   *
   * @return the fetch type: lazy or eager
   */
  FetchType fetch() default FetchType.LAZY;

  /** (Optional, and applicable only for a boolean association) Defines the name of the class of which this object is an element
   * when the boolean association holds true.
   *
   * @return  a string class URI
   */
  String trueClass() default "";

  /** (Optional, and applicable only for a boolean association) Defines the name of the class of which this object is an element
   * when the boolean association holds false.
   *
   * @return  a string class URI
   */
  String falseClass() default "";

  /** (Optional, and applicable only for a Map association) Defines the type name of the Map key.
   *
   * @return  a string class URI
   */
  String mapKeyType() default "";

  /** (Optional, and applicable only for a Map association) Defines the type name of the Map value.
   *
   * @return  a string class URI
   */
  String mapValueType() default "";

}
