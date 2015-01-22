/*
 * RDFNamespace.java
 *
 * Created on August 23, 2007, 9:19 PM
 *
 * Description: .
 *
 * Copyright (C) August 23, 2007 Stephen L. Reed.
 */

package org.texai.kb.persistence;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** This interface defines the @RDFNamespace annotation and its properties.
 *
 * @author reed
 */
@Target(TYPE)
@Documented
@Retention(RUNTIME)
public @interface RDFNamespace {

    /** Gets the namespace prefix.
     * @return the namespace prefix
     */
    String prefix();

    /** Gets the namespace URI.
     * @return the namespace URI
     */
    String namespaceURI();
}
