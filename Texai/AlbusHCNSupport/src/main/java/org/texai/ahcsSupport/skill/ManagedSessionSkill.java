/*
 * RDFEntity.java
 *
 * Created on November 1, 2011, 1:10 PM
 *
 * Description: Indicates that the annotated class is a managed session skill.
 *
 * Copyright (C) November 1, 2011 Stephen L. Reed.
 */

package org.texai.ahcsSupport.skill;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** This interface defines the @RDFEntity annotation and its properties.
 *
 * @author reed
 */
@Target(TYPE)
@Documented
@Retention(RUNTIME)
public @interface ManagedSessionSkill {
}
