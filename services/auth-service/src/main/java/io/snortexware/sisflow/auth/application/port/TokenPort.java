package io.snortexware.sisflow.auth.application.port;

import java.util.UUID;

public interface TokenPort {
    String generateAccessToken(UUID userId, UUID tenantId);
}
