/*
 * RDFEntity.java
 *
 * Created on August 3, 2007, 2:31 PM
 *
 * Description: Indicates that the annotated class is an RDF entity, whose
 * associations are mapped into the RDF store.  If the term does
 * not currently exist in the RDF store, it will be created.
 *
 * Copyright (C) August 3, 2007 Stephen L. Reed.
 */

package org.texai.kb.persistence;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/** This interface defines the @RDFEntity annotation and its properties.
 *
 * @author reed
 */
@Target(TYPE)
@Documented
@Retention(RUNTIME)
public @interface RDFEntity {

  /** (Optional) Defines the pairs of RDF namespace prefix and namespace URI used within this RDF entity
   *
   * @return a string array of name
   */
  RDFNamespace[] namespaces() default { };

  /** (Optional) Defines RDF class URI.
   *
   * @return the URI as a string
   */
  String subject() default "";

  /** (Optional) Defines the types of this entity.  Each value must specify an existing RDF class URI.
   *
   * @return a string array of type URIs
   */
  String[] type() default {"cyc:ObjectType"};

  /** (Optional) Defines the RDF super classes of this entity.  Each value must specify an existing RDF class URI.
   *
   * @return a string array of subClassOf URIs
   */
  String[] subClassOf() default {"cyc:AbstractInformationStructure"};

  /** (Optional) Defines the RDF persistence context URI.
   *
   * @return a string array of context URIs
   */
  String context() default "";

}
