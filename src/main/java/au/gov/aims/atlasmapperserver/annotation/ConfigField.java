/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

	// Set to true to make the field Read Only on the demo version.
	boolean demoReadOnly() default false;
}
