package io.snortexware.sisflow.dto;

import io.snortexware.sisflow.entities.System;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateSystemRequest {

    @NotBlank
    private String name;

    private String description;

    private String version;

    private String url;

    private System.Status status;
}
