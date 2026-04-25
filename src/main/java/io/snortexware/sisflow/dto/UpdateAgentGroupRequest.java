package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateAgentGroupRequest {

    @NotBlank
    private String name;

    private String description;
}
