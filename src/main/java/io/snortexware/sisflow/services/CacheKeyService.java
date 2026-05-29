package io.snortexware.sisflow.services;

import io.snortexware.sisflow.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Service("cacheKeyService")
@RequiredArgsConstructor
public class CacheKeyService {

    private final TenantContext tenantContext;

    public String tenantKey(Object... parts) {
        UUID tenantId = tenantContext.getCurrentTenant();
        String tenantScope = tenantId == null ? "global" : tenantId.toString();

        if (parts == null || parts.length == 0) {
            return tenantScope;
        }

        return tenantScope + ":" + Arrays.stream(parts)
                .map(String::valueOf)
                .collect(Collectors.joining(":"));
    }
}
