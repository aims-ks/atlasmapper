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
@Target(ElementType.TYPE)
// Allow access to the annotion using reflexion
@Retention(RetentionPolicy.RUNTIME)

public @interface Module {
	String name() default "";
	String title() default "";
	String description() default "";
}
