package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionValidationRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Permission code is required")
    private String permissionCode;
}
