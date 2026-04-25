package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAgentGroupRequest {

    @NotBlank
    private String name;

    private String description;
}
