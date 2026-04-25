package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequest {

    @NotBlank(message = "Role code is required")
    private String code;

    @NotBlank(message = "Role name is required")
    private String name;

    private String description;

    @NotNull(message = "Hierarchy level is required")
    private Integer hierarchyLevel;
}
