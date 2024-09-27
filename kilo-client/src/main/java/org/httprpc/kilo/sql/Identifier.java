package org.httprpc.kilo.sql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a property is part of the identifier for an entity.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Identifier {
    /**
     * The relative order of the property within the identifier.
     */
    int value() default 0;
}
