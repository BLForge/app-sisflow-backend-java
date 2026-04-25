package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkRoleAssignmentRequest {

    @NotEmpty(message = "User IDs list cannot be empty")
    private List<UUID> userIds;

    @NotNull(message = "Role ID is required")
    private UUID roleId;
}
