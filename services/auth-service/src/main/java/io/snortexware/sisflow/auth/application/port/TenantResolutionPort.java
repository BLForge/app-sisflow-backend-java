package io.snortexware.sisflow.auth.application.port;

import io.snortexware.sisflow.auth.domain.model.Tenant;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public interface TenantResolutionPort {
    Optional<Tenant> resolve(HttpServletRequest request);
}
