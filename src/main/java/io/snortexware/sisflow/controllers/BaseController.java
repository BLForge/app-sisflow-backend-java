package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.services.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Shared auth guard helpers. Extend this in controllers that need role checks.
 */
public abstract class BaseController {

    protected abstract AuthorizationService authorizationService();

    protected void requireModerator(UUID callerId) {
        if (callerId == null || !authorizationService().isModeratorOrAbove(callerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
    }

    protected void requireAdmin(UUID callerId) {
        if (callerId == null || !authorizationService().isAdminOrAbove(callerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
    }

    protected void requireDeveloper(UUID callerId) {
        if (callerId == null || !authorizationService().isDeveloperOrAbove(callerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
    }
}
