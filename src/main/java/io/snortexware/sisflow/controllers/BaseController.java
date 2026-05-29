package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;

import java.util.UUID;

public abstract class BaseController {

    protected abstract AuthorizationService authorizationService();

    protected void requireModerator(UUID callerId) {
        if (callerId == null || !authorizationService().isModeratorOrAbove(callerId))
            throw AppException.forbidden();
    }

    protected void requireAdmin(UUID callerId) {
        if (callerId == null || !authorizationService().isAdminOrAbove(callerId))
            throw AppException.forbidden();
    }

    protected void requireDeveloper(UUID callerId) {
        if (callerId == null || !authorizationService().isDeveloperOrAbove(callerId))
            throw AppException.forbidden();
    }
}
