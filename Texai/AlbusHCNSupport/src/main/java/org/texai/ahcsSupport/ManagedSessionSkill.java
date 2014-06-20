/*
 * RDFEntity.java
 *
 * Created on November 1, 2011, 1:10 PM
 *
 * Description: Indicates that the annotated class is a managed session skill.
 *
 * Copyright (C) November 1, 2011 Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.texai.ahcsSupport;

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
