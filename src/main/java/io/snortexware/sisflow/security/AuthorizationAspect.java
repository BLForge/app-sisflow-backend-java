package io.snortexware.sisflow.security;

import io.snortexware.sisflow.security.annotations.RequireAllPermissions;
import io.snortexware.sisflow.security.annotations.RequireAllRoles;
import io.snortexware.sisflow.security.annotations.RequireAnyPermission;
import io.snortexware.sisflow.security.annotations.RequireAnyRole;
import io.snortexware.sisflow.security.annotations.RequirePermission;
import io.snortexware.sisflow.security.annotations.RequireRole;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * AOP aspect for intercepting methods with authorization annotations.
 * Validates roles and permissions before method execution.
 */
@Slf4j
@Aspect
@Component
public class AuthorizationAspect {

    private final AuthorizationValidator authorizationValidator;

    public AuthorizationAspect(AuthorizationValidator authorizationValidator) {
        this.authorizationValidator = authorizationValidator;
    }

    /**
     * Intercept methods annotated with @RequireRole.
     */
    @Before("@annotation(requireRole)")
    public void checkRequireRole(JoinPoint joinPoint, RequireRole requireRole) {
        log.debug("Checking @RequireRole: {}", requireRole.value());
        authorizationValidator.requireRole(requireRole.value());
    }

    /**
     * Intercept methods annotated with @RequireAnyRole.
     */
    @Before("@annotation(requireAnyRole)")
    public void checkRequireAnyRole(JoinPoint joinPoint, RequireAnyRole requireAnyRole) {
        List<String> roles = Arrays.asList(requireAnyRole.value());
        log.debug("Checking @RequireAnyRole: {}", roles);
        authorizationValidator.requireAnyRole(roles);
    }

    /**
     * Intercept methods annotated with @RequireAllRoles.
     */
    @Before("@annotation(requireAllRoles)")
    public void checkRequireAllRoles(JoinPoint joinPoint, RequireAllRoles requireAllRoles) {
        List<String> roles = Arrays.asList(requireAllRoles.value());
        log.debug("Checking @RequireAllRoles: {}", roles);
        authorizationValidator.requireAllRoles(roles);
    }

    /**
     * Intercept methods annotated with @RequirePermission.
     */
    @Before("@annotation(requirePermission)")
    public void checkRequirePermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        log.debug("Checking @RequirePermission: {}", requirePermission.value());
        authorizationValidator.requirePermission(requirePermission.value());
    }

    /**
     * Intercept methods annotated with @RequireAnyPermission.
     */
    @Before("@annotation(requireAnyPermission)")
    public void checkRequireAnyPermission(JoinPoint joinPoint, RequireAnyPermission requireAnyPermission) {
        List<String> permissions = Arrays.asList(requireAnyPermission.value());
        log.debug("Checking @RequireAnyPermission: {}", permissions);
        authorizationValidator.requireAnyPermission(permissions);
    }

    /**
     * Intercept methods annotated with @RequireAllPermissions.
     */
    @Before("@annotation(requireAllPermissions)")
    public void checkRequireAllPermissions(JoinPoint joinPoint, RequireAllPermissions requireAllPermissions) {
        List<String> permissions = Arrays.asList(requireAllPermissions.value());
        log.debug("Checking @RequireAllPermissions: {}", permissions);
        authorizationValidator.requireAllPermissions(permissions);
    }
}
