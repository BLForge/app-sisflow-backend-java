package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePermissionRequest {

    @NotBlank(message = "Permission code is required")
    private String code;

    @NotBlank(message = "Permission name is required")
    private String name;

    private String description;

    @NotBlank(message = "Permission category is required")
    private String category;
}
