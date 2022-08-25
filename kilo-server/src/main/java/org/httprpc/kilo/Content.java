package org.httprpc.kilo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Associates custom body content with a service method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Content {
    /**
     * @return
     * The content type.
     */
    Class<?> type();

    /**
     * @return
     * <code>true</code> if the body is expected to contain a list of values of
     * the given type; <code>false</code>, if the body will contain a single
     * value.
     */
    boolean multiple() default false;
}
