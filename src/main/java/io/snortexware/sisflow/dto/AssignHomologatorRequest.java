package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignHomologatorRequest {
    @NotNull
    private UUID userId;
}
