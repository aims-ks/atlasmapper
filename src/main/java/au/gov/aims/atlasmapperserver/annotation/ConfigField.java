/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.org.au>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.gov.aims.atlasmapperserver.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author glafond
 */
// This annotation apply on fields
@Target(ElementType.FIELD)
// Allow access to the annotion using reflexion
@Retention(RetentionPolicy.RUNTIME)

// Definition of the annotation.
// Adding fields here allow them to be set when using the annotation.
//
// For example, if we define a String field named "name" here,
// we can use the annotation like this:
// @ConfigField(name="someName")
// private String myField;
public @interface ConfigField {
	String name() default "";
	String getter() default "";
	String setter() default "";
	// List of other possible name for this field, for backward compatibility.
	String alias() default "";

	// Set to true to make the field Read Only on the demo version.
	boolean demoReadOnly() default false;
}
