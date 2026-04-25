package io.snortexware.sisflow.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to require any of the specified permissions for method access.
 * Used with AuthorizationAspect for AOP-based authorization.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAnyPermission {
    String[] value();
}
