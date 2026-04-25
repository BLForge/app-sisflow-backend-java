package io.snortexware.sisflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionValidationResponse {

    private UUID userId;

    private String permissionCode;

    private boolean hasPermission;
}
